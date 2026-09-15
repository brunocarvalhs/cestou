package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateLanguageUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class WelcomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authConfig = mockk<AuthConfigUseCase>(relaxed = true)
    private val updateLanguageUseCase = mockk<UpdateLanguageUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init tracks a welcome screen_view`() {
        WelcomeViewModel(authConfig, updateLanguageUseCase, analyticsTracker)

        verify { analyticsTracker.trackScreenView("welcome", "WelcomeViewModel") }
    }

    @Test
    fun `onSignInFailure tracks the failure reason from the exception message`() {
        val vm = WelcomeViewModel(authConfig, updateLanguageUseCase, analyticsTracker)

        vm.intent.onSignInFailure(IllegalStateException("network error"))

        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.AUTH_SIGN_IN_FAILED, mapOf("reason" to "network error"))
        }
    }

    @Test
    fun `onLanguageSelected persists the chosen language`() {
        val vm = WelcomeViewModel(authConfig, updateLanguageUseCase, analyticsTracker)

        vm.intent.onLanguageSelected("pt-BR")

        coVerify { updateLanguageUseCase("pt-BR") }
    }

    @Test
    fun `onSignInFailure falls back to the exception class name when there is no message`() {
        val vm = WelcomeViewModel(authConfig, updateLanguageUseCase, analyticsTracker)

        vm.intent.onSignInFailure(IllegalStateException())

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AUTH_SIGN_IN_FAILED,
                mapOf("reason" to "IllegalStateException")
            )
        }
    }
}
