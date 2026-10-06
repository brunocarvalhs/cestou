package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.CredentialRules

internal enum class EmailFieldError { EMPTY, INVALID, ALREADY_IN_USE }

internal enum class PasswordFieldError { EMPTY, TOO_SHORT }

internal enum class NameField { GIVEN_NAME, FAMILY_NAME }

internal enum class SignUpField { GIVEN_NAME, FAMILY_NAME, EMAIL, PASSWORD }

internal fun emailFieldError(email: String): EmailFieldError? = when {
    email.isBlank() -> EmailFieldError.EMPTY
    !CredentialRules.isValidEmail(email) -> EmailFieldError.INVALID
    else -> null
}

internal fun passwordFieldError(password: String): PasswordFieldError? = when {
    password.isEmpty() -> PasswordFieldError.EMPTY
    password.length < CredentialRules.MIN_PASSWORD_LENGTH -> PasswordFieldError.TOO_SHORT
    else -> null
}
