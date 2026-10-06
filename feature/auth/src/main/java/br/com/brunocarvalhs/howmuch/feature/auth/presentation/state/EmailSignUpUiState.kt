package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameResult
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameValidator
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError

@Immutable
internal data class EmailSignUpUiState(
    val givenName: String = "",
    val familyName: String = "",
    val email: String = "",
    val password: String = "",
    val givenNameError: PersonNameError? = null,
    val familyNameError: PersonNameError? = null,
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val formError: EmailAuthError? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
) {
    /** Disabled while any field is empty or invalid, and while a request is in flight (spec EPA-02). */
    val isSubmitEnabled: Boolean
        get() = !isLoading &&
            PersonNameValidator.validate(givenName, familyName) is PersonNameResult.Valid &&
            emailFieldError(email) == null &&
            passwordFieldError(password) == null
}
