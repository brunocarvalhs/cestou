package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state.WelcomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
internal class WelcomeViewModel @Inject constructor(
    val authConfig: AuthConfigUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        analyticsTracker.trackScreenView(screenName = "welcome", screenClass = "WelcomeViewModel")
    }

    fun onSignInFailure(exception: Exception) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AUTH_SIGN_IN_FAILED,
            mapOf(AnalyticsParams.REASON to (exception.message ?: exception::class.simpleName.orEmpty()))
        )
    }
}
