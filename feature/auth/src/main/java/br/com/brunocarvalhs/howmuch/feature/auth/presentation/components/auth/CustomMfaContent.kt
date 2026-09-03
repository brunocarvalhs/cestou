package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep

@Composable
internal fun CustomMfaEnrollmentContent(state: MfaEnrollmentContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Configurar Segundo Fator (MFA)",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (state.step) {
            MfaEnrollmentStep.SelectFactor -> {
                Text(text = "Escolha como deseja receber seus códigos de segurança.")
                // List available factors...
            }
            MfaEnrollmentStep.ConfigureSms -> {
                CestouTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    label = "Telefone para MFA"
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = "Enviar SMS", onClick = state.onSendSmsCodeClick)
            }
            MfaEnrollmentStep.ConfigureTotp -> {
                Text(text = "Escaneie o QR Code ou insira o código manual no seu app de autenticação.")
                Text(text = "Segredo: ${state.totpSecret?.sharedSecretKey}")
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = "Já escaneei", onClick = state.onContinueToVerifyClick)
            }
            MfaEnrollmentStep.VerifyFactor -> {
                CestouTextField(
                    value = state.verificationCode,
                    onValueChange = state.onVerificationCodeChange,
                    label = "Código de Verificação"
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = "Confirmar", onClick = state.onVerifyClick)
            }
            MfaEnrollmentStep.ShowRecoveryCodes -> {
                Text(text = "Guarde estes códigos em um lugar seguro!")
                state.recoveryCodes?.forEach { Text(text = it) }
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = "Concluído", onClick = state.onCodesSavedClick)
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun CustomMfaChallengeContent(state: MfaChallengeContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Verificação em Duas Etapas",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Insira o código enviado para seu ${state.factorType}")

        Spacer(modifier = Modifier.height(16.dp))

        CestouTextField(
            value = state.verificationCode,
            onValueChange = state.onVerificationCodeChange,
            label = "Código MFA"
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            CestouButton(
                text = "Verificar",
                onClick = state.onVerifyClick,
                enabled = state.isValid
            )
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
