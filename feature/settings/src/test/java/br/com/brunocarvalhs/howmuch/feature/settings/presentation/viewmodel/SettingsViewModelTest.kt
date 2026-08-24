package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateCurrencyUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateLanguageUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel.SettingsViewModel
import br.com.brunocarvalhs.howmuch.feature.settings.commons.navigation.ThemeSettings
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val updateLanguageUseCase = mockk<UpdateLanguageUseCase>(relaxed = true)
    private val updateCurrencyUseCase = mockk<UpdateCurrencyUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    private fun viewModel(): SettingsViewModel {
        every { getSettingsUseCase() } returns flowOf(AppSettings())
        return SettingsViewModel(context, getSettingsUseCase, updateLanguageUseCase, updateCurrencyUseCase, analyticsTracker).also {
            it.setNavigator(navigator)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onUpdateLanguage updates the language and tracks the event`() = runTest {
        coEvery { updateLanguageUseCase("en") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onUpdateLanguage("en")

        coVerify { updateLanguageUseCase("en") }
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.SETTINGS_LANGUAGE_CHANGED, mapOf("language" to "en"))
        }
    }

    @Test
    fun `onUpdateCurrency updates the currency and tracks the event`() = runTest {
        coEvery { updateCurrencyUseCase("USD") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onUpdateCurrency("USD")

        coVerify { updateCurrencyUseCase("USD") }
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.SETTINGS_CURRENCY_CHANGED, mapOf("currency" to "USD"))
        }
    }

    @Test
    fun `onNavigate forwards the route to the navigator`() {
        val vm = viewModel()

        vm.intent.onNavigate(ThemeSettings)

        verify { navigator.navigate(ThemeSettings) }
    }

    @Test
    fun `onBack navigates back`() {
        val vm = viewModel()

        vm.intent.onBack()

        verify { navigator.goBack() }
    }
}
