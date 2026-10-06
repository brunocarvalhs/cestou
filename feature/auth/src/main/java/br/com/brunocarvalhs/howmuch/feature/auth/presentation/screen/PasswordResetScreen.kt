package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormScaffold
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthSubmitButton
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthTextLink
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.messageRes
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.PasswordResetIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordResetUiState

/** Spec EPA-04: same confirmation whether or not the account exists; resend after 30 s. */
@Composable
internal fun PasswordResetScreen(
    state: PasswordResetUiState,
    intent: PasswordResetIntent,
    onBack: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    AuthFormScaffold(
        title = stringResource(R.string.auth_reset_title),
        subtitle = stringResource(R.string.auth_reset_description),
        onBack = onBack
    ) {
        AuthFormError(error = state.formError, testTag = "reset_form_error")

        if (state.isSent) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_sent_message")
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.auth_reset_sent),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        CestouTextField(
            value = state.email,
            onValueChange = intent.onEmailChange,
            label = stringResource(R.string.auth_field_email),
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = {
                focusManager.clearFocus()
                if (state.isSubmitEnabled) intent.onSubmit()
            }),
            isError = state.emailError != null,
            errorMessage = state.emailError?.let { stringResource(it.messageRes()) },
            enabled = !state.isLoading,
            testTag = "reset_email_field",
            textFieldModifier = Modifier.semantics { contentType = ContentType.EmailAddress }
        )

        AuthSubmitButton(
            text = when {
                state.resendCountdownSeconds > 0 ->
                    stringResource(R.string.auth_reset_resend_in, state.resendCountdownSeconds)
                state.isSent -> stringResource(R.string.auth_reset_resend)
                else -> stringResource(R.string.auth_reset_button)
            },
            enabled = state.isSubmitEnabled,
            isLoading = state.isLoading,
            testTag = "reset_submit_button",
            onClick = intent.onSubmit
        )

        AuthTextLink(
            text = stringResource(R.string.auth_back_to_sign_in),
            testTag = "reset_back_to_sign_in",
            onClick = onBack
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetScreenPreview() {
    CestouTheme {
        PasswordResetScreen(
            state = PasswordResetUiState(email = "ana@test.com", isSent = true, resendCountdownSeconds = 24),
            intent = PasswordResetIntent()
        )
    }
}
