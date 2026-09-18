package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Phone
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
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.auth.R
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
            text = stringResource(R.string.auth_phone_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        val loadingDescription = stringResource(R.string.auth_loading_content_description)

        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> {
                CestouTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    label = stringResource(R.string.auth_label_phone),
                    leadingIcon = Icons.Default.Phone,
                    placeholder = stringResource(R.string.auth_phone_placeholder)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription }
                    )
                } else {
                    CestouButton(
                        text = stringResource(R.string.auth_action_send_code),
                        onClick = state.onSendCodeClick
                    )
                }
            }
            PhoneAuthStep.EnterVerificationCode -> {
                Text(text = stringResource(R.string.auth_phone_code_sent_to, state.fullPhoneNumber))
                Spacer(modifier = Modifier.height(16.dp))

                CestouTextField(
                    value = state.verificationCode,
                    onValueChange = state.onVerificationCodeChange,
                    label = stringResource(R.string.auth_label_verification_code)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription }
                    )
                } else {
                    CestouButton(
                        text = stringResource(R.string.auth_action_verify_code),
                        onClick = state.onVerifyCodeClick
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.auth_phone_resend_timer, state.resendTimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
