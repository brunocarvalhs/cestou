package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase.AuthConfigUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WelcomeViewModelTest {

    private val authConfig = mockk<AuthConfigUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    @Test
    fun `init tracks a welcome screen_view`() {
        WelcomeViewModel(authConfig, analyticsTracker)

        verify { analyticsTracker.trackScreenView("welcome", "WelcomeViewModel") }
    }

    @Test
    fun `onSignInFailure tracks the failure reason from the exception message`() {
        val vm = WelcomeViewModel(authConfig, analyticsTracker)

        vm.intent.onSignInFailure(IllegalStateException("network error"))

        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.AUTH_SIGN_IN_FAILED, mapOf("reason" to "network error"))
        }
    }

    @Test
    fun `onSignInFailure falls back to the exception class name when there is no message`() {
        val vm = WelcomeViewModel(authConfig, analyticsTracker)

        vm.intent.onSignInFailure(IllegalStateException())

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AUTH_SIGN_IN_FAILED,
                mapOf("reason" to "IllegalStateException")
            )
        }
    }
}
