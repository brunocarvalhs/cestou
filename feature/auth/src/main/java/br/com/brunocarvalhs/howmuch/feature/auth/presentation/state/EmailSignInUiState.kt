package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError

@Immutable
internal data class EmailSignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val formError: EmailAuthError? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
) {
    val isSubmitEnabled: Boolean
        get() = !isLoading && emailFieldError(email) == null && password.isNotEmpty()
}
