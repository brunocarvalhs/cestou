package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.Settings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init tracks screen_view and loads the current user profile`() {
        val user = AuthenticatedUser(id = "u1")
        every { authService.currentUser } returns user
        coEvery { userRepository.getUserProfile("u1") } returns flowOf(UserProfile(id = "u1", name = "Ana"))

        val vm = ProfileViewModel(authService, userRepository, analyticsTracker)

        verify { analyticsTracker.trackScreenView("profile", "ProfileViewModel") }
        assertEquals(user, vm.uiState.value.user)
    }

    @Test
    fun `init does not observe a profile when there is no current user`() {
        every { authService.currentUser } returns null

        ProfileViewModel(authService, userRepository, analyticsTracker)

        io.mockk.verify(exactly = 0) { userRepository.getUserProfile(any()) }
    }

    @Test
    fun `onNavigate opens the Settings screen`() {
        every { authService.currentUser } returns null
        val vm = ProfileViewModel(authService, userRepository, analyticsTracker)
        vm.setNavigator(navigator)

        vm.intent.onNavigate(Unit)

        verify { navigator.navigate(Settings) }
    }

    @Test
    fun `onSignOut signs out and tracks the event`() = runTest {
        every { authService.currentUser } returns null
        coEvery { authService.signOut() } returns Result.success(Unit)
        val vm = ProfileViewModel(authService, userRepository, analyticsTracker)

        vm.intent.onSignOut()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.PROFILE_SIGN_OUT) }
    }
}
