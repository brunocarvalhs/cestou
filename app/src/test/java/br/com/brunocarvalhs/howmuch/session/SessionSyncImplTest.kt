package br.com.brunocarvalhs.howmuch.session

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.ChatTurnTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionSyncImplTest {

    private val authState = MutableStateFlow<AuthenticatedUser?>(null)
    private val authService = mockk<AuthService> {
        every { this@mockk.authState } returns this@SessionSyncImplTest.authState
    }
    private val userRepository = mockk<UserRepository>()
    private val wearableSyncService = mockk<WearableSyncService>(relaxed = true)
    private val chatTurnTracker = mockk<ChatTurnTracker>(relaxed = true)
    private val aiSession = mockk<AiSession>(relaxed = true)

    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private fun sessionSync() = SessionSyncImpl(
        authService,
        userRepository,
        wearableSyncService,
        chatTurnTracker,
        aiSession
    )

    private fun signedIn(name: String? = "Ana", photo: String? = null) =
        AuthenticatedUser(id = "uid-1", displayName = name, photoUrl = photo)

    @Test
    fun `saves the profile of a signed-in account that has a name`() {
        coEvery { userRepository.saveProfile(any()) } returns Result.success(Unit)
        authState.value = signedIn(name = "Ana", photo = "https://photo")

        sessionSync().start(scope)

        coVerify(exactly = 1) {
            userRepository.saveProfile(UserProfile(id = "uid-1", name = "Ana", photoUrl = "https://photo"))
        }
    }

    @Test
    fun `does not save a profile for an account without a name`() {
        authState.value = signedIn(name = null)

        sessionSync().start(scope)

        coVerify(exactly = 0) { userRepository.saveProfile(any()) }
    }

    @Test
    fun `a failed profile save is logged and does not break the sync`() {
        coEvery { userRepository.saveProfile(any()) } returns Result.failure(IllegalStateException("offline"))
        authState.value = signedIn()

        sessionSync().start(scope)

        // Still observing: the next sign-out must still clean up the watch.
        authState.value = null
        coVerify(exactly = 1) { wearableSyncService.notifySignOut() }
    }

    @Test
    fun `signing out cleans up the watch, cancels AI turns and clears the AI history`() {
        coEvery { userRepository.saveProfile(any()) } returns Result.success(Unit)
        authState.value = signedIn()
        sessionSync().start(scope)

        authState.value = null

        coVerify(exactly = 1) { wearableSyncService.notifySignOut() }
        coVerify(exactly = 1) { wearableSyncService.syncSubscription(isPro = false) }
        verify(exactly = 1) { chatTurnTracker.cancelAll() }
        verify(exactly = 1) { aiSession.clearHistory() }
    }

    @Test
    fun `signing in does not run the sign-out cleanup`() {
        coEvery { userRepository.saveProfile(any()) } returns Result.success(Unit)
        sessionSync().start(scope)

        authState.value = signedIn()

        coVerify(exactly = 0) { wearableSyncService.notifySignOut() }
        verify(exactly = 0) { aiSession.clearHistory() }
        verify(exactly = 0) { chatTurnTracker.cancelAll() }
    }

    @Test
    fun `the initial state does not count as a sign-out`() {
        authState.value = null

        sessionSync().start(scope)

        coVerify(exactly = 0) { wearableSyncService.notifySignOut() }
        assertEquals(null, authState.value)
    }
}
