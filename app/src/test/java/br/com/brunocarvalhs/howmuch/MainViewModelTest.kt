package br.com.brunocarvalhs.howmuch

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

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
        val getSettingsUseCase = mockk<GetSettingsUseCase>()
        every { getSettingsUseCase() } returns flowOf(settings)
        every { authService.authState } returns MutableStateFlow(currentUser)
        every { authService.currentUser } returns currentUser
        return MainViewModel(getSettingsUseCase, authService, analyticsTracker)
    }

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
}
