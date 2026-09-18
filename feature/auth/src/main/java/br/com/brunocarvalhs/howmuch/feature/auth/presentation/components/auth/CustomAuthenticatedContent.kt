package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext

@Composable
internal fun CustomAuthenticatedContent(
    state: AuthState,
    uiContext: AuthSuccessUiContext
) {
    when (state) {
        is AuthState.Success -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.auth_success_welcome),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.auth_success_signed_in_as,
                        state.user.email ?: state.user.uid
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                CestouButton(text = stringResource(R.string.auth_action_manage_mfa), onClick = uiContext.onManageMfa)
                Spacer(modifier = Modifier.height(16.dp))
                CestouButton(
                    text = stringResource(R.string.auth_action_sign_out),
                    onClick = uiContext.onSignOut,
                    containerColor = MaterialTheme.colorScheme.error
                )
            }
        }

        is AuthState.RequiresEmailVerification -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.auth_verify_email_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.auth_verify_email_description, state.email),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                CestouButton(text = stringResource(R.string.auth_action_already_verified), onClick = uiContext.onReloadUser)
                Spacer(modifier = Modifier.height(16.dp))
                CestouButton(
                    text = stringResource(R.string.auth_action_resend_email),
                    onClick = { state.user.sendEmailVerification() }
                )
            }
        }

        is AuthState.RequiresProfileCompletion -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.auth_complete_profile_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.auth_complete_profile_missing_fields,
                        state.missingFields.joinToString()
                    )
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val loadingDescription = stringResource(R.string.auth_loading_content_description)
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription }
                )
            }
        }
    }
}
