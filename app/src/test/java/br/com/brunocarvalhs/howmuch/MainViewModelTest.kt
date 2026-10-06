package br.com.brunocarvalhs.howmuch

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val userRepository = mockk<UserRepository> {
        coEvery { saveProfile(any()) } returns Result.success(Unit)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        settings: AppSettings = AppSettings(),
        currentUser: AuthenticatedUser? = null
    ): MainViewModel {
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.getSettings() } returns flowOf(settings)
        authState.value = currentUser
        every { authService.authState } returns authState
        every { authService.currentUser } answers { authState.value }
        return MainViewModel(settingsRepository, authService, userRepository, analyticsTracker)
    }

    private val authState = MutableStateFlow<AuthenticatedUser?>(null)

    @Test
    fun `init tracks app_open`() {
        viewModel()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.APP_OPEN) }
    }

    @Test
    fun `isAuthenticated is false when there is no current user`() = runTest {
        val vm = viewModel(currentUser = null)

        vm.isAuthenticated.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isAuthenticated is true when there is a current user`() = runTest {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1"))

        vm.isAuthenticated.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `themeMode and language reflect the current settings`() = runTest {
        val vm = viewModel(settings = AppSettings(themeMode = ThemeMode.DARK, language = "en"))

        vm.themeMode.test { assertEquals(ThemeMode.DARK, awaitItem()) }
        vm.language.test { assertEquals("en", awaitItem()) }
    }

    @Test
    fun `photoUrl reflects the current user's photo`() = runTest {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1", photoUrl = "http://x/y.png"))

        vm.photoUrl.test { assertEquals("http://x/y.png", awaitItem()) }
    }

    // region required name (spec EPA-06/07) and profile sync

    @Test
    fun `requiresName is true for a signed-in user without a display name`() = runTest {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = " "))

        vm.requiresName.test { assertTrue(awaitItem()) }
        assertTrue(vm.requiresNameNow())
    }

    @Test
    fun `requiresName is false for a named user and for a signed-out user`() = runTest {
        val named = viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = "Ana Silva"))
        named.requiresName.test { assertFalse(awaitItem()) }
        assertFalse(named.requiresNameNow())

        val signedOut = viewModel(currentUser = null)
        signedOut.requiresName.test { assertFalse(awaitItem()) }
        assertFalse(signedOut.requiresNameNow())
    }

    @Test
    fun `requiresNameNow reads the latest auth state, not a stale flow value`() {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1"))

        authState.value = AuthenticatedUser(id = "u1", displayName = "Ana Silva")

        assertFalse(vm.requiresNameNow())
    }

    @Test
    fun `saves the users profile for a signed-in user with a name, without the e-mail`() {
        viewModel(
            currentUser = AuthenticatedUser(
                id = "u1", email = "ana@test.com", displayName = "Ana Silva", photoUrl = "http://x/y.png"
            )
        )

        coVerify(exactly = 1) {
            userRepository.saveProfile(UserProfile(id = "u1", name = "Ana Silva", photoUrl = "http://x/y.png"))
        }
    }

    @Test
    fun `saves the profile again only when name or photo change`() {
        viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = "Ana Silva"))

        authState.value = AuthenticatedUser(id = "u1", displayName = "Ana Silva", email = "new@test.com")
        authState.value = AuthenticatedUser(id = "u1", displayName = "Ana Souza")

        coVerify(exactly = 2) { userRepository.saveProfile(any()) }
        coVerify { userRepository.saveProfile(UserProfile(id = "u1", name = "Ana Souza")) }
    }

    @Test
    fun `does not save a profile without a name or without a user`() {
        viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = null))
        authState.value = null

        coVerify(exactly = 0) { userRepository.saveProfile(any()) }
    }

    @Test
    fun `a failed profile save does not crash the app`() {
        coEvery { userRepository.saveProfile(any()) } returns Result.failure(IllegalStateException("offline"))

        viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = "Ana Silva"))
    }

    // endregion
}
