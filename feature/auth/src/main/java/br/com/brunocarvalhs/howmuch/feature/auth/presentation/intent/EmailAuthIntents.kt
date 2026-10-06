package br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent

import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.NameField
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.SignUpField

internal data class EmailSignUpIntent(
    val onGivenNameChange: (String) -> Unit = {},
    val onFamilyNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onFieldFocusLost: (SignUpField) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onFormErrorDismissed: () -> Unit = {},
)

internal data class EmailSignInIntent(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onEmailFocusLost: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onFormErrorDismissed: () -> Unit = {},
)

internal data class PasswordResetIntent(
    val onEmailChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
)

internal data class CompleteNameIntent(
    val onGivenNameChange: (String) -> Unit = {},
    val onFamilyNameChange: (String) -> Unit = {},
    val onFieldFocusLost: (NameField) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onUseAnotherAccount: () -> Unit = {},
)
