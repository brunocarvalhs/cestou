package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingSessionHolder
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.auth.domain.wear.PairingHandshake
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingStage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val wearableSyncService = mockk<WearableSyncService>(relaxed = true)
    private val inbox = WearPairingInbox()
    private val holder = PairingSessionHolder()
    private val authState = MutableStateFlow<AuthenticatedUser?>(null)
    private val authService = mockk<AuthService> { every { authState } returns this@PairingViewModelTest.authState }
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private val sessionId = slot<String>()
    private val watchKey = slot<String>()

    private lateinit var viewModel: PairingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        // A relaxed mock answers false for Boolean results: the happy path needs a real success.
        coEvery { wearableSyncService.sendWearConfirm(any(), any()) } returns true
        viewModel = PairingViewModel(wearableSyncService, inbox, holder, authService, analyticsTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the phone being unreachable ends the session with that reason`() = runTest(dispatcher) {
        coEvery { wearableSyncService.requestPhonePairing(any(), any()) } returns false

        viewModel.start()

        waitUntil { viewModel.uiState.value.stage == PairingStage.ERROR }
        assertEquals(PairingFailure.PHONE_UNREACHABLE, viewModel.uiState.value.failure)
    }

    @Test
    fun `start while a session runs does not ask the phone again`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()

        viewModel.start()
        waitUntil { watchKey.isCaptured }
        viewModel.start()
        runCurrent()

        coVerify(exactly = 1) { wearableSyncService.requestPhonePairing(any(), any()) }
    }

    @Test
    fun `watch and phone derive the same code`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        viewModel.start()
        waitUntil { watchKey.isCaptured }

        val phone = PairingHandshake.newKeyPair()
        val watchPublic = Base64.getUrlDecoder().decode(watchKey.captured)
        val phonePublic = phone.public.encoded
        val expected = PairingHandshake.deriveCode(
            sharedSecret = PairingHandshake.sharedSecret(phone.private, watchPublic),
            wearPublicKey = watchPublic,
            phonePublicKey = phonePublic
        )
        inbox.emit(
            WearPairingMessage.PhoneKey(
                sessionId = sessionId.captured,
                phonePublicKey = PairingHandshake.encodePublicKey(phone),
                sourceNodeId = PHONE_NODE
            )
        )

        waitUntil { viewModel.uiState.value.stage == PairingStage.CODE }
        assertEquals(expected, viewModel.uiState.value.code)
        assertEquals(8 + 1, viewModel.uiState.value.code.length)
    }

    @Test
    fun `confirming tells the phone and accepts the sign-in for this session`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        viewModel.start()
        waitUntil { watchKey.isCaptured }
        emitPhoneKey()
        waitUntil { viewModel.uiState.value.stage == PairingStage.CODE }

        viewModel.confirm()

        waitUntil { viewModel.uiState.value.stage == PairingStage.SYNCING }
        coVerify { wearableSyncService.sendWearConfirm(PHONE_NODE, sessionId.captured) }
        assertTrue(holder.isConfirmed(sessionId.captured))
    }

    @Test
    fun `a failed confirmation send ends the session`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        coEvery { wearableSyncService.sendWearConfirm(any(), any()) } returns false
        viewModel.start()
        waitUntil { watchKey.isCaptured }
        emitPhoneKey()
        waitUntil { viewModel.uiState.value.stage == PairingStage.CODE }

        viewModel.confirm()

        waitUntil { viewModel.uiState.value.stage == PairingStage.ERROR }
        assertEquals(PairingFailure.PHONE_UNREACHABLE, viewModel.uiState.value.failure)
        assertFalse(holder.isConfirmed(sessionId.captured))
    }

    @Test
    fun `no phone key within 30 seconds times out`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        viewModel.start()
        waitUntil { watchKey.isCaptured }

        advanceTimeBy(PHONE_KEY_TIMEOUT_MS + 1)
        runCurrent()

        assertEquals(PairingStage.ERROR, viewModel.uiState.value.stage)
        assertEquals(PairingFailure.TIMEOUT, viewModel.uiState.value.failure)
    }

    @Test
    fun `a failure reported by the phone is shown and not sent back`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        viewModel.start()
        waitUntil { watchKey.isCaptured }

        inbox.emit(WearPairingMessage.Error(sessionId.captured, PairingFailure.ACCOUNT_MISMATCH.name))

        waitUntil { viewModel.uiState.value.stage == PairingStage.ERROR }
        assertEquals(PairingFailure.ACCOUNT_MISMATCH, viewModel.uiState.value.failure)
        coVerify(exactly = 0) { wearableSyncService.sendPairingError(any(), any(), any()) }
    }

    @Test
    fun `a phone key that is not a valid public key ends the session`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        viewModel.start()
        waitUntil { watchKey.isCaptured }

        inbox.emit(WearPairingMessage.PhoneKey(sessionId.captured, "not-a-key", PHONE_NODE))

        waitUntil { viewModel.uiState.value.stage == PairingStage.ERROR }
        assertEquals(PairingFailure.INVALID_KEY, viewModel.uiState.value.failure)
    }

    @Test
    fun `messages for another session are ignored`() = runTest(dispatcher) {
        givenPhoneAnswersRequests()
        viewModel.start()
        waitUntil { watchKey.isCaptured }

        inbox.emit(WearPairingMessage.Error("another-session", PairingFailure.TIMEOUT.name))
        runCurrent()

        assertEquals(PairingStage.CONNECTING, viewModel.uiState.value.stage)
    }

    private fun givenPhoneAnswersRequests() {
        coEvery { wearableSyncService.requestPhonePairing(capture(sessionId), capture(watchKey)) } returns true
    }

    private fun emitPhoneKey() {
        val phone = PairingHandshake.newKeyPair()
        inbox.emit(
            WearPairingMessage.PhoneKey(
                sessionId = sessionId.captured,
                phonePublicKey = PairingHandshake.encodePublicKey(phone),
                sourceNodeId = PHONE_NODE
            )
        )
    }

    /**
     * Key generation and the shared-secret step run on Dispatchers.Default, so their results come back
     * from another thread. Poll the condition, letting the test dispatcher run what has arrived.
     */
    private fun TestScope.waitUntil(condition: () -> Boolean) {
        repeat(MAX_POLLS) {
            runCurrent()
            if (condition()) return
            Thread.sleep(POLL_DELAY_MS)
        }
        runCurrent()
        assertTrue("Condition was not reached", condition())
    }

    private companion object {
        const val PHONE_NODE = "phone-node"
        const val PHONE_KEY_TIMEOUT_MS = 30_000L
        const val MAX_POLLS = 500
        const val POLL_DELAY_MS = 5L
    }
}
