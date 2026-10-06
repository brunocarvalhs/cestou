package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouPasswordField
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormScaffold
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthSubmitButton
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthTextLink
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.PersonNameFields
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.messageRes
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.onFocusLost
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.EmailSignUpIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailSignUpUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.SignUpField

/** Spec EPA-02: First name, Last name, e-mail, password. Every field required, none optional. */
@Composable
internal fun EmailSignUpScreen(
    state: EmailSignUpUiState,
    intent: EmailSignUpIntent,
    onBack: () -> Unit = {},
    onGoToSignIn: (email: String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val enabled = !state.isLoading

    AuthFormScaffold(
        title = stringResource(R.string.auth_sign_up_title),
        subtitle = stringResource(R.string.auth_sign_up_subtitle),
        onBack = onBack
    ) {
        AuthFormError(error = state.formError, testTag = "signup_form_error")

        PersonNameFields(
            givenName = state.givenName,
            familyName = state.familyName,
            givenNameError = state.givenNameError,
            familyNameError = state.familyNameError,
            enabled = enabled,
            testTagPrefix = "signup",
            onGivenNameChange = intent.onGivenNameChange,
            onFamilyNameChange = intent.onFamilyNameChange,
            onGivenNameFocusLost = { intent.onFieldFocusLost(SignUpField.GIVEN_NAME) },
            onFamilyNameFocusLost = { intent.onFieldFocusLost(SignUpField.FAMILY_NAME) },
        )

        CestouTextField(
            value = state.email,
            onValueChange = intent.onEmailChange,
            label = stringResource(R.string.auth_field_email),
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = state.emailError != null,
            errorMessage = state.emailError?.let { stringResource(it.messageRes()) },
            enabled = enabled,
            testTag = "signup_email_field",
            textFieldModifier = Modifier
                .onFocusLost { intent.onFieldFocusLost(SignUpField.EMAIL) }
                .semantics { contentType = ContentType.EmailAddress }
        )
        if (state.emailError == EmailFieldError.ALREADY_IN_USE) {
            AuthTextLink(
                text = stringResource(R.string.email_error_in_use_action),
                testTag = "signup_email_in_use_sign_in_action",
                onClick = { onGoToSignIn(state.email.trim()) }
            )
        }

        CestouPasswordField(
            value = state.password,
            onValueChange = intent.onPasswordChange,
            label = stringResource(R.string.auth_field_password),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (state.isSubmitEnabled) intent.onSubmit()
            }),
            isError = state.passwordError != null,
            errorMessage = state.passwordError?.let { stringResource(it.messageRes()) },
            supportingText = stringResource(R.string.auth_password_rule),
            enabled = enabled,
            testTag = "signup_password_field",
            textFieldModifier = Modifier
                .onFocusLost { intent.onFieldFocusLost(SignUpField.PASSWORD) }
                .semantics { contentType = ContentType.NewPassword }
        )

        AuthSubmitButton(
            text = stringResource(R.string.auth_create_account),
            enabled = state.isSubmitEnabled,
            isLoading = state.isLoading,
            testTag = "signup_submit_button",
            onClick = intent.onSubmit
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.auth_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AuthTextLink(
                text = stringResource(R.string.auth_sign_in_button),
                testTag = "signup_go_to_sign_in",
                onClick = { onGoToSignIn(state.email.trim()) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailSignUpScreenPreview() {
    CestouTheme {
        EmailSignUpScreen(
            state = EmailSignUpUiState(
                givenName = "aaaa",
                givenNameError = PersonNameError.NOT_A_NAME,
                email = "ana@test.com",
                emailError = EmailFieldError.ALREADY_IN_USE
            ),
            intent = EmailSignUpIntent()
        )
    }
}
