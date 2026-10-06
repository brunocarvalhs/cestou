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
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouPasswordField
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormScaffold
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthSubmitButton
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthTextLink
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.messageRes
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.onFocusLost
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.EmailSignInIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailSignInUiState

/** Spec EPA-03. Wrong password and unknown account share one generic message. */
@Composable
internal fun EmailSignInScreen(
    state: EmailSignInUiState,
    intent: EmailSignInIntent,
    onBack: () -> Unit = {},
    onForgotPassword: (email: String) -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val enabled = !state.isLoading

    AuthFormScaffold(
        title = stringResource(R.string.auth_sign_in_title),
        subtitle = null,
        onBack = onBack
    ) {
        AuthFormError(error = state.formError, testTag = "signin_form_error")

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
            testTag = "signin_email_field",
            textFieldModifier = Modifier
                .onFocusLost(intent.onEmailFocusLost)
                .semantics { contentType = ContentType.EmailAddress + ContentType.Username }
        )

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
            enabled = enabled,
            testTag = "signin_password_field",
            textFieldModifier = Modifier.semantics { contentType = ContentType.Password }
        )

        AuthTextLink(
            text = stringResource(R.string.auth_forgot_password),
            testTag = "signin_forgot_password",
            onClick = { onForgotPassword(state.email.trim()) }
        )

        AuthSubmitButton(
            text = stringResource(R.string.auth_sign_in_button),
            enabled = state.isSubmitEnabled,
            isLoading = state.isLoading,
            testTag = "signin_submit_button",
            onClick = intent.onSubmit
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.auth_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AuthTextLink(
                text = stringResource(R.string.auth_create_account),
                testTag = "signin_create_account",
                onClick = onCreateAccount
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailSignInScreenPreview() {
    CestouTheme {
        EmailSignInScreen(
            state = EmailSignInUiState(
                email = "ana@test.com",
                password = "12345678",
                formError = EmailAuthError.INVALID_CREDENTIALS
            ),
            intent = EmailSignInIntent()
        )
    }
}
