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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@Composable
internal fun CustomMfaEnrollmentContent(state: MfaEnrollmentContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.mfa_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (state.step) {
            MfaEnrollmentStep.SelectFactor -> {
                Text(text = stringResource(R.string.mfa_setup_choose_method))
                // List available factors...
            }
            MfaEnrollmentStep.ConfigureSms -> {
                CestouTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    label = stringResource(R.string.mfa_phone_label)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(R.string.mfa_send_sms), onClick = state.onSendSmsCodeClick)
            }
            MfaEnrollmentStep.ConfigureTotp -> {
                Text(text = stringResource(R.string.mfa_totp_instructions))
                Text(text = stringResource(R.string.mfa_totp_secret, state.totpSecret?.sharedSecretKey.orEmpty()))
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(R.string.mfa_scanned), onClick = state.onContinueToVerifyClick)
            }
            MfaEnrollmentStep.VerifyFactor -> {
                CestouTextField(
                    value = state.verificationCode,
                    onValueChange = state.onVerificationCodeChange,
                    label = stringResource(R.string.mfa_verification_code_label)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(CoreR.string.action_confirm), onClick = state.onVerifyClick)
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
            text = stringResource(R.string.mfa_two_step_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.mfa_enter_code_sent, state.factorType))

        Spacer(modifier = Modifier.height(16.dp))

        CestouTextField(
            value = state.verificationCode,
            onValueChange = state.onVerificationCodeChange,
            label = stringResource(R.string.mfa_code_label)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            CestouButton(
                text = stringResource(R.string.mfa_verify),
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
