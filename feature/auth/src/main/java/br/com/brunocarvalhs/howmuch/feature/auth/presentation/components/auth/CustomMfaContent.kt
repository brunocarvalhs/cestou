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
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep

@Composable
private fun InlineError(message: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
internal fun CustomMfaEnrollmentContent(state: MfaEnrollmentContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.auth_mfa_enrollment_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (state.step) {
            MfaEnrollmentStep.SelectFactor -> {
                Text(text = stringResource(R.string.auth_mfa_select_factor))
                // List available factors...
            }
            MfaEnrollmentStep.ConfigureSms -> {
                CestouTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    label = stringResource(R.string.auth_label_phone_mfa)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(R.string.auth_action_send_sms), onClick = state.onSendSmsCodeClick)
            }
            MfaEnrollmentStep.ConfigureTotp -> {
                Text(text = stringResource(R.string.auth_mfa_totp_instructions))
                Text(text = stringResource(R.string.auth_mfa_totp_secret, state.totpSecret?.sharedSecretKey.orEmpty()))
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(R.string.auth_action_already_scanned), onClick = state.onContinueToVerifyClick)
            }
            MfaEnrollmentStep.VerifyFactor -> {
                CestouTextField(
                    value = state.verificationCode,
                    onValueChange = state.onVerificationCodeChange,
                    label = stringResource(R.string.auth_label_verification_code)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(CoreUiR.string.action_confirm), onClick = state.onVerifyClick)
            }
            MfaEnrollmentStep.ShowRecoveryCodes -> {
                Text(text = stringResource(R.string.auth_mfa_recovery_codes_warning))
                state.recoveryCodes?.forEach { Text(text = it) }
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(text = stringResource(R.string.auth_action_done), onClick = state.onCodesSavedClick)
            }
        }

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            val loadingDescription = stringResource(R.string.auth_loading_content_description)
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = loadingDescription }
            )
        }

        state.error?.let { InlineError(it) }
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
            text = stringResource(R.string.auth_mfa_challenge_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.auth_mfa_challenge_description, state.factorType.toString()))

        Spacer(modifier = Modifier.height(16.dp))

        CestouTextField(
            value = state.verificationCode,
            onValueChange = state.onVerificationCodeChange,
            label = stringResource(R.string.auth_label_mfa_code)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            val loadingDescription = stringResource(R.string.auth_loading_content_description)
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = loadingDescription }
            )
        } else {
            CestouButton(
                text = stringResource(R.string.auth_action_verify),
                onClick = state.onVerifyClick,
                enabled = state.isValid
            )
        }

        state.error?.let { InlineError(it) }
    }
}
