package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouPasswordField
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode

@Composable
internal fun CustomEmailContent(state: EmailAuthContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (state.mode) {
                EmailAuthMode.SignIn -> stringResource(R.string.auth_email_title_sign_in)
                EmailAuthMode.SignUp -> stringResource(R.string.auth_email_title_sign_up)
                EmailAuthMode.ResetPassword -> stringResource(R.string.auth_email_title_reset_password)
                EmailAuthMode.EmailLinkSignIn -> stringResource(R.string.auth_email_title_link_sign_in)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.mode != EmailAuthMode.EmailLinkSignIn) {
            CestouTextField(
                value = state.email,
                onValueChange = state.onEmailChange,
                label = stringResource(R.string.auth_label_email),
                leadingIcon = Icons.Default.Email
            )

            if (state.mode != EmailAuthMode.ResetPassword) {
                Spacer(modifier = Modifier.height(16.dp))
                CestouPasswordField(
                    value = state.password,
                    onValueChange = state.onPasswordChange,
                    label = stringResource(R.string.auth_label_password)
                )
            }

            if (state.mode == EmailAuthMode.SignUp) {
                Spacer(modifier = Modifier.height(16.dp))
                CestouPasswordField(
                    value = state.confirmPassword,
                    onValueChange = state.onConfirmPasswordChange,
                    label = stringResource(R.string.auth_label_confirm_password)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CestouTextField(
                    value = state.displayName,
                    onValueChange = state.onDisplayNameChange,
                    label = stringResource(R.string.auth_label_display_name)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            val loadingDescription = stringResource(R.string.auth_loading_content_description)
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = loadingDescription
                }
            )
        } else {
            CestouButton(
                text = when (state.mode) {
                    EmailAuthMode.SignIn -> stringResource(R.string.auth_action_sign_in)
                    EmailAuthMode.SignUp -> stringResource(R.string.auth_action_sign_up)
                    EmailAuthMode.ResetPassword -> stringResource(R.string.auth_action_send_reset_link)
                    EmailAuthMode.EmailLinkSignIn -> stringResource(R.string.auth_action_verify_email)
                },
                onClick = {
                    when (state.mode) {
                        EmailAuthMode.SignIn -> state.onSignInClick()
                        EmailAuthMode.SignUp -> state.onSignUpClick()
                        EmailAuthMode.ResetPassword -> state.onSendResetLinkClick()
                        EmailAuthMode.EmailLinkSignIn -> { /* Automatic */ }
                    }
                }
            )
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
