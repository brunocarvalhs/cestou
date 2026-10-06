package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError

@Immutable
internal data class PasswordResetUiState(
    val email: String = "",
    val emailError: EmailFieldError? = null,
    val formError: EmailAuthError? = null,
    val isLoading: Boolean = false,
    /** Same confirmation whether or not the account exists (spec EPA-04). */
    val isSent: Boolean = false,
    val resendCountdownSeconds: Int = 0,
) {
    val isSubmitEnabled: Boolean
        get() = !isLoading && resendCountdownSeconds == 0 && email.isNotBlank()
}
