package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.ui.extensions.appVersionName
import br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.WelcomeUiState
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class WelcomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val authConfig: AuthConfigUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        WelcomeUiState(
            version = context.applicationContext.appVersionName()
        )
    )
    val uiState = _uiState.asStateFlow()

    val intent = WelcomeIntent(
        onSignInFailure = { exception ->
            onSignInFailure(exception)
        },
        onLanguageSelected = { languageCode ->
            updateLanguage(languageCode)
        }
    )

    init {
        analyticsTracker.trackScreenView(screenName = "welcome", screenClass = "WelcomeViewModel")
    }

    private fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            updateLanguageUseCase(languageCode)
        }
    }

    private fun onSignInFailure(exception: Exception) {

        analyticsTracker.trackEvent(
            AnalyticsEvents.AUTH_SIGN_IN_FAILED,
            mapOf(
                AnalyticsParams.REASON to (exception.message
                    ?: exception::class.simpleName.orEmpty())
            )
        )
    }
}
