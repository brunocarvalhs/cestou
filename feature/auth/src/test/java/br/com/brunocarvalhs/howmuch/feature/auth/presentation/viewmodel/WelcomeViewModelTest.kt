package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.AuthErrorType
import com.firebase.ui.auth.AuthException
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.UnknownHostException

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

    @Test
    fun `onSignInFailure classifies a no-connectivity exception into the ui state`() {
        val vm = WelcomeViewModel(authConfig, analyticsTracker)

        vm.intent.onSignInFailure(UnknownHostException("Unable to resolve host"))

        assertEquals(AuthErrorType.NO_CONNECTIVITY, vm.uiState.value.error)
    }

    @Test
    fun `onSignInFailure classifies an AuthException NetworkException as a connection failure`() {
        val vm = WelcomeViewModel(authConfig, analyticsTracker)

        vm.intent.onSignInFailure(AuthException.NetworkException(message = "network error"))

        assertEquals(AuthErrorType.CONNECTION_FAILURE, vm.uiState.value.error)
    }

    @Test
    fun `onSignInFailure classifies an unrecognized exception as structural`() {
        val vm = WelcomeViewModel(authConfig, analyticsTracker)

        vm.intent.onSignInFailure(IllegalStateException("boom"))

        assertEquals(AuthErrorType.STRUCTURAL, vm.uiState.value.error)
    }

    @Test
    fun `onDismissError clears the error from the ui state`() {
        val vm = WelcomeViewModel(authConfig, analyticsTracker)
        vm.intent.onSignInFailure(IllegalStateException("boom"))

        vm.intent.onDismissError()

        assertNull(vm.uiState.value.error)
    }
}
