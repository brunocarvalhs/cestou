package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep

@Composable
internal fun CustomPhoneContent(state: PhoneAuthContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Autenticação por Telefone",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> {
                CestouTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    label = "Telefone",
                    leadingIcon = Icons.Default.Phone,
                    placeholder = "(00) 00000-0000"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    CestouButton(
                        text = "Enviar Código",
                        onClick = state.onSendCodeClick
                    )
                }
            }
            PhoneAuthStep.EnterVerificationCode -> {
                Text(text = "Enviamos um código para ${state.fullPhoneNumber}")
                Spacer(modifier = Modifier.height(16.dp))
                
                CestouTextField(
                    value = state.verificationCode,
                    onValueChange = state.onVerificationCodeChange,
                    label = "Código de Verificação"
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    CestouButton(
                        text = "Verificar Código",
                        onClick = state.onVerifyCodeClick
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Reenviar código em ${state.resendTimer}s")
            }
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
