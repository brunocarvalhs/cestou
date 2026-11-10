package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui.GoogleIdTokenProvider
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingStage
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneWearPairingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val wearableSyncService = mockk<WearableSyncService>(relaxed = true)
    private val watchConfirmations = MutableSharedFlow<WearPairingMessage.WatchConfirmed>(extraBufferCapacity = 4)
    private val user = AuthenticatedUser(id = USER_ID, email = EMAIL, displayName = null, photoUrl = null)
    private val authService = mockk<AuthService>(relaxed = true) { every { currentUser } returns user }
    private val credentialProvider = mockk<GoogleIdTokenProvider>()
    private val featureFlags = mockk<FeatureFlagService> { every { isEnabled(any(), any()) } returns true }
    private val subscriptionRepository = mockk<SubscriptionRepository> {
        every { observeStatus() } returns flowOf(SubscriptionStatus.PRO)
    }
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private lateinit var viewModel: PhoneWearPairingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { wearableSyncService.observeWatchConfirm(any()) } returns watchConfirmations
        // A relaxed mock answers false for Boolean results: the happy path needs real successes.
        coEvery { wearableSyncService.sendPhoneKey(any(), any(), any()) } returns true
        coEvery { wearableSyncService.sendAuthPair(any(), any(), any(), any()) } returns true
        viewModel = PhoneWearPairingViewModel(
            wearableSyncService = wearableSyncService,
            authService = authService,
            googleIdTokenProvider = credentialProvider,
            featureFlagService = featureFlags,
            subscriptionRepository = subscriptionRepository,
            analyticsTracker = analyticsTracker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a disabled flag answers the watch with DISABLED`() = runTest(dispatcher) {
        every { featureFlags.isEnabled(any(), any()) } returns false

        viewModel.start(SESSION, watchKey(), WATCH_NODE)

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.ERROR }
        assertEquals(PairingFailure.DISABLED, viewModel.uiState.value.failure)
        coVerify { wearableSyncService.sendPairingError(WATCH_NODE, SESSION, PairingFailure.DISABLED.name) }
    }

    @Test
    fun `a user without Pro is answered with NOT_PRO`() = runTest(dispatcher) {
        every { subscriptionRepository.observeStatus() } returns flowOf(SubscriptionStatus.FREE)

        viewModel.start(SESSION, watchKey(), WATCH_NODE)

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.ERROR }
        assertEquals(PairingFailure.NOT_PRO, viewModel.uiState.value.failure)
    }

    @Test
    fun `a signed-out phone is answered with NOT_SIGNED_IN`() = runTest(dispatcher) {
        every { authService.currentUser } returns null

        viewModel.start(SESSION, watchKey(), WATCH_NODE)

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.ERROR }
        assertEquals(PairingFailure.NOT_SIGNED_IN, viewModel.uiState.value.failure)
    }

    @Test
    fun `the phone shows the code and sends its key to the watch node`() = runTest(dispatcher) {
        givenPhoneCanStartPairing()

        viewModel.start(SESSION, watchKey(), WATCH_NODE)

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.CODE }
        assertEquals(9, viewModel.uiState.value.code.length)
        coVerify { wearableSyncService.sendPhoneKey(WATCH_NODE, SESSION, any()) }
    }

    @Test
    fun `the sign-in is sent only after both sides confirmed, watch last`() = runTest(dispatcher) {
        givenPhoneCanStartPairing()
        viewModel.start(SESSION, watchKey(), WATCH_NODE)
        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.CODE }

        viewModel.confirm(context)
        waitUntil { viewModel.uiState.value.isPhoneConfirmed }
        coVerify(exactly = 0) { wearableSyncService.sendAuthPair(any(), any(), any(), any()) }

        watchConfirmations.emit(WearPairingMessage.WatchConfirmed(SESSION, WATCH_NODE))

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.DONE }
        coVerify(exactly = 1) { wearableSyncService.sendAuthPair(WATCH_NODE, SESSION, USER_ID, ID_TOKEN) }
    }

    @Test
    fun `the sign-in is sent once when the watch confirms first`() = runTest(dispatcher) {
        givenPhoneCanStartPairing()
        viewModel.start(SESSION, watchKey(), WATCH_NODE)
        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.CODE }
        waitUntil { watchConfirmations.subscriptionCount.value > 0 }

        watchConfirmations.emit(WearPairingMessage.WatchConfirmed(SESSION, WATCH_NODE))
        runCurrent()
        viewModel.confirm(context)

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.DONE }
        coVerify(exactly = 1) { wearableSyncService.sendAuthPair(any(), any(), any(), any()) }
    }

    @Test
    fun `an account other than the app's is refused and the watch is told`() = runTest(dispatcher) {
        givenPhoneCanStartPairing(credentialEmail = "other@example.com")
        viewModel.start(SESSION, watchKey(), WATCH_NODE)
        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.CODE }

        viewModel.confirm(context)

        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.ERROR }
        assertEquals(PairingFailure.ACCOUNT_MISMATCH, viewModel.uiState.value.failure)
        coVerify(exactly = 0) { wearableSyncService.sendAuthPair(any(), any(), any(), any()) }
        coVerify { wearableSyncService.sendPairingError(WATCH_NODE, SESSION, PairingFailure.ACCOUNT_MISMATCH.name) }
    }

    @Test
    fun `the watch not confirming within the timeout ends the session`() = runTest(dispatcher) {
        givenPhoneCanStartPairing()
        viewModel.start(SESSION, watchKey(), WATCH_NODE)
        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.CODE }
        viewModel.confirm(context)
        waitUntil { viewModel.uiState.value.isPhoneConfirmed }

        advanceTimeBy(WATCH_CONFIRM_TIMEOUT_MS + 1)
        runCurrent()

        assertEquals(PairingFailure.TIMEOUT, viewModel.uiState.value.failure)
        coVerify(exactly = 0) { wearableSyncService.sendAuthPair(any(), any(), any(), any()) }
    }

    @Test
    fun `cancelling tells the watch and sends nothing else`() = runTest(dispatcher) {
        givenPhoneCanStartPairing()
        viewModel.start(SESSION, watchKey(), WATCH_NODE)
        waitUntil { viewModel.uiState.value.stage == PhoneWearPairingStage.CODE }

        viewModel.cancel()
        runCurrent()

        coVerify { wearableSyncService.sendPairingError(WATCH_NODE, SESSION, PairingFailure.CANCELED.name) }
        coVerify(exactly = 0) { wearableSyncService.sendAuthPair(any(), any(), any(), any()) }
    }

    private fun givenPhoneCanStartPairing(credentialEmail: String = EMAIL) {
        val credential = mockk<GoogleIdTokenCredential> {
            every { id } returns credentialEmail
            every { idToken } returns ID_TOKEN
        }
        coEvery { credentialProvider.get(any()) } returns credential
    }

    /** A real watch public key, so the phone's ECDH step runs for real. */
    private fun watchKey(): String =
        br.com.brunocarvalhs.howmuch.feature.auth.domain.wear.PairingHandshake
            .encodePublicKey(br.com.brunocarvalhs.howmuch.feature.auth.domain.wear.PairingHandshake.newKeyPair())

    /** Key generation runs on Dispatchers.Default; poll until its result has been handled. */
    private fun TestScope.waitUntil(condition: () -> Boolean) {
        repeat(MAX_POLLS) {
            runCurrent()
            if (condition()) return
            Thread.sleep(POLL_DELAY_MS)
        }
        runCurrent()
        assertTrue("Condition was not reached, state=${viewModel.uiState.value}", condition())
    }

    private companion object {
        const val SESSION = "session-1"
        const val WATCH_NODE = "watch-node"
        const val USER_ID = "user-1"
        const val EMAIL = "user@example.com"
        const val ID_TOKEN = "id-token"
        const val WATCH_CONFIRM_TIMEOUT_MS = 60_000L
        const val MAX_POLLS = 500
        const val POLL_DELAY_MS = 5L
    }
}
