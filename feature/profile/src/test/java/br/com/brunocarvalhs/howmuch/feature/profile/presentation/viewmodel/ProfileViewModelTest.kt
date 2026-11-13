package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val userRepository = mockk<UserRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val wearableSyncService = mockk<WearableSyncService>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init merges the Firestore profile into the user state`() {
        val user = AuthenticatedUser(id = "u1", email = "old@example.com")
        every { authService.currentUser } returns user
        coEvery { userRepository.getUserProfile("u1") } returns flowOf(
            UserProfile(id = "u1", name = "Ana", photoUrl = "https://example.com/ana.png")
        )

        val vm = ProfileViewModel(authService, userRepository, analyticsTracker, wearableSyncService)

        verify(exactly = 0) { analyticsTracker.trackScreenView(any(), any()) }
        assertEquals(
            user.copy(displayName = "Ana", photoUrl = "https://example.com/ana.png"),
            vm.uiState.value.user
        )
    }

    @Test
    fun `a new Firestore profile emission updates the exposed user state`() = runTest {
        val user = AuthenticatedUser(id = "u1", displayName = "Old Name")
        val profileFlow = MutableSharedFlow<UserProfile?>(replay = 1)
        every { authService.currentUser } returns user
        coEvery { userRepository.getUserProfile("u1") } returns profileFlow

        val vm = ProfileViewModel(authService, userRepository, analyticsTracker, wearableSyncService)
        assertEquals("Old Name", vm.uiState.value.user?.displayName)

        // Simulates a profile edit made from a second device, synced through Firestore.
        profileFlow.emit(UserProfile(id = "u1", name = "Edited On Other Device"))

        assertEquals("Edited On Other Device", vm.uiState.value.user?.displayName)
        assertEquals("u1", vm.uiState.value.user?.id)
    }

    @Test
    fun `init does not observe a profile when there is no current user`() {
        every { authService.currentUser } returns null

        ProfileViewModel(authService, userRepository, analyticsTracker, wearableSyncService)

        io.mockk.verify(exactly = 0) { userRepository.getUserProfile(any()) }
    }

    @Test
    fun `onSignOut signs out and tracks the event`() = runTest {
        every { authService.currentUser } returns null
        coEvery { authService.signOut() } returns Result.success(Unit)
        val vm = ProfileViewModel(authService, userRepository, analyticsTracker, wearableSyncService)

        vm.intent.onSignOut()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.PROFILE_SIGN_OUT) }
    }
}
