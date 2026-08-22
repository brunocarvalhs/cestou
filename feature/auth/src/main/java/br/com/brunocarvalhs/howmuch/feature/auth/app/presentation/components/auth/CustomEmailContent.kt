package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouPasswordField
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
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
                EmailAuthMode.SignIn -> "Acessar Conta"
                EmailAuthMode.SignUp -> "Criar Conta"
                EmailAuthMode.ResetPassword -> "Recuperar Senha"
                EmailAuthMode.EmailLinkSignIn -> "Entrar com Link"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.mode != EmailAuthMode.EmailLinkSignIn) {
            CestouTextField(
                value = state.email,
                onValueChange = state.onEmailChange,
                label = "E-mail",
                leadingIcon = Icons.Default.Email
            )

            if (state.mode != EmailAuthMode.ResetPassword) {
                Spacer(modifier = Modifier.height(16.dp))
                CestouPasswordField(
                    value = state.password,
                    onValueChange = state.onPasswordChange,
                    label = "Senha"
                )
            }

            if (state.mode == EmailAuthMode.SignUp) {
                Spacer(modifier = Modifier.height(16.dp))
                CestouPasswordField(
                    value = state.confirmPassword,
                    onValueChange = state.onConfirmPasswordChange,
                    label = "Confirmar Senha"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CestouTextField(
                    value = state.displayName,
                    onValueChange = state.onDisplayNameChange,
                    label = "Nome de Exibição"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            CestouButton(
                text = when (state.mode) {
                    EmailAuthMode.SignIn -> "Entrar"
                    EmailAuthMode.SignUp -> "Cadastrar"
                    EmailAuthMode.ResetPassword -> "Enviar Link"
                    EmailAuthMode.EmailLinkSignIn -> "Verificar E-mail"
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
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
