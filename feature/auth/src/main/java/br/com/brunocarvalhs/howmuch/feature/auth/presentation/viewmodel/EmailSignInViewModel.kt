package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.EmailSignInIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailSignInUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.emailFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EmailSignInViewModel @Inject constructor(
    private val repository: EmailAuthRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailSignInUiState())
    val uiState = _uiState.asStateFlow()
    private var started = false

    val intent = EmailSignInIntent(
        onEmailChange = { value -> _uiState.update { it.copy(email = value, emailError = null, formError = null) } },
        onPasswordChange = { value ->
            _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
        },
        onEmailFocusLost = { _uiState.update { it.copy(emailError = emailFieldError(it.email)) } },
        onSubmit = ::submit,
        onFormErrorDismissed = { _uiState.update { it.copy(formError = null) } },
    )

    /** Applies the e-mail handed over by another screen once; never overwrites what was typed. */
    fun start(prefilledEmail: String) {
        if (started) return
        started = true
        _uiState.update { it.copy(email = prefilledEmail) }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!state.isSubmitEnabled) {
            _uiState.update {
                it.copy(
                    emailError = emailFieldError(it.email),
                    passwordError = PasswordFieldError.EMPTY.takeIf { _ -> it.password.isEmpty() },
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            repository.signIn(state.email.trim(), state.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { throwable ->
                    val error = (throwable as? EmailAuthException)?.error ?: EmailAuthError.UNKNOWN
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.AUTH_SIGN_IN_FAILED,
                        mapOf(AnalyticsParams.REASON to error.name, AnalyticsParams.SOURCE to "email")
                    )
                    _uiState.update {
                        if (error == EmailAuthError.INVALID_EMAIL) {
                            it.copy(isLoading = false, emailError = EmailFieldError.INVALID)
                        } else {
                            it.copy(isLoading = false, formError = error)
                        }
                    }
                }
        }
    }
}
