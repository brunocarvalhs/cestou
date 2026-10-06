package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.PasswordResetIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordResetUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.emailFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PasswordResetViewModel @Inject constructor(
    private val repository: EmailAuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState = _uiState.asStateFlow()
    private var started = false

    val intent = PasswordResetIntent(
        onEmailChange = { value -> _uiState.update { it.copy(email = value, emailError = null, formError = null) } },
        onSubmit = ::submit,
    )

    fun start(prefilledEmail: String) {
        if (started) return
        started = true
        _uiState.update { it.copy(email = prefilledEmail) }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isSubmitEnabled) return
        val emailError = emailFieldError(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            repository.sendPasswordReset(state.email.trim())
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSent = true) }
                    startResendCountdown()
                }
                .onFailure { throwable ->
                    val error = (throwable as? EmailAuthException)?.error ?: EmailAuthError.UNKNOWN
                    _uiState.update { it.copy(isLoading = false, formError = error) }
                }
        }
    }

    private suspend fun startResendCountdown() {
        for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
            _uiState.update { it.copy(resendCountdownSeconds = seconds) }
            delay(ONE_SECOND_MS)
        }
        _uiState.update { it.copy(resendCountdownSeconds = 0) }
    }

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 30
        const val ONE_SECOND_MS = 1_000L
    }
}
