package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateThemeUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel.ThemeSettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
class ThemeSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val updateThemeUseCase = mockk<UpdateThemeUseCase>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns flowOf(AppSettings(themeMode = ThemeMode.DARK))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads the current theme from the repository`() {
        val vm = ThemeSettingsViewModel(getSettingsUseCase, updateThemeUseCase)

        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun `onUpdateTheme forwards to the use case`() = runTest {
        coEvery { updateThemeUseCase(ThemeMode.LIGHT) } returns Result.success(Unit)
        val vm = ThemeSettingsViewModel(getSettingsUseCase, updateThemeUseCase)

        vm.intent.onUpdateTheme(ThemeMode.LIGHT)

        coVerify { updateThemeUseCase(ThemeMode.LIGHT) }
    }

    @Test
    fun `onBack navigates back`() {
        val vm = ThemeSettingsViewModel(getSettingsUseCase, updateThemeUseCase)
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }
}
