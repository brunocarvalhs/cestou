package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameResult
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameValidator
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.EmailSignUpIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailSignUpUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.SignUpField
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.emailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.passwordFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EmailSignUpViewModel @Inject constructor(
    private val repository: EmailAuthRepository,
    private val authService: AuthService,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailSignUpUiState())
    val uiState = _uiState.asStateFlow()

    val intent = EmailSignUpIntent(
        onGivenNameChange = { value -> _uiState.update { it.copy(givenName = value, givenNameError = null) } },
        onFamilyNameChange = { value -> _uiState.update { it.copy(familyName = value, familyNameError = null) } },
        onEmailChange = { value -> _uiState.update { it.copy(email = value, emailError = null) } },
        onPasswordChange = { value -> _uiState.update { it.copy(password = value, passwordError = null) } },
        onFieldFocusLost = ::validateField,
        onSubmit = ::submit,
        onFormErrorDismissed = { _uiState.update { it.copy(formError = null) } },
    )

    private fun validateField(field: SignUpField) {
        val state = _uiState.value
        when (field) {
            SignUpField.GIVEN_NAME -> {
                val error = nameErrors(state.givenName, state.familyName).first
                analyticsTracker.trackNameError(error, SOURCE_NAME)
                _uiState.update { it.copy(givenNameError = error) }
            }
            SignUpField.FAMILY_NAME -> {
                val error = nameErrors(state.givenName, state.familyName).second
                analyticsTracker.trackNameError(error, SOURCE_NAME)
                _uiState.update { it.copy(familyNameError = error) }
            }
            SignUpField.EMAIL -> _uiState.update { it.copy(emailError = emailFieldError(it.email)) }
            SignUpField.PASSWORD -> _uiState.update { it.copy(passwordError = passwordFieldError(it.password)) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val name = PersonNameValidator.validate(state.givenName, state.familyName)
        if (name !is PersonNameResult.Valid || !state.isSubmitEnabled) {
            val (givenError, familyError) = nameErrors(state.givenName, state.familyName)
            _uiState.update {
                it.copy(
                    givenNameError = givenError,
                    familyNameError = familyError,
                    emailError = emailFieldError(it.email),
                    passwordError = passwordFieldError(it.password),
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            repository.signUp(state.email.trim(), state.password)
                .onSuccess {
                    // A failure here leaves an account without a name; the app's required-name
                    // gate sends it to the name step, so sign-up still counts as done.
                    authService.updateDisplayName(name.fullName)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { onSignUpFailed(it) }
        }
    }

    private fun onSignUpFailed(throwable: Throwable) {
        val error = (throwable as? EmailAuthException)?.error ?: EmailAuthError.UNKNOWN
        analyticsTracker.trackEvent(
            AnalyticsEvents.AUTH_SIGN_UP_FAILED,
            mapOf(AnalyticsParams.REASON to error.name, AnalyticsParams.SOURCE to SOURCE_EMAIL)
        )
        _uiState.update {
            when (error) {
                EmailAuthError.EMAIL_ALREADY_IN_USE -> it.copy(emailError = EmailFieldError.ALREADY_IN_USE)
                EmailAuthError.INVALID_EMAIL -> it.copy(emailError = EmailFieldError.INVALID)
                EmailAuthError.WEAK_PASSWORD -> it.copy(passwordError = PasswordFieldError.TOO_SHORT)
                else -> it.copy(formError = error)
            }.copy(isLoading = false)
        }
    }

    private companion object {
        const val SOURCE_EMAIL = "email"
        const val SOURCE_NAME = "sign_up"
    }
}
