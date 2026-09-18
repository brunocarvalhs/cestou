package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.AuthState

@Composable
internal fun CustomReauthContent(
    state: AuthState.ReauthenticationRequired,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.auth_reauth_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.reason ?: stringResource(R.string.auth_reauth_default_reason),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Note: FirebaseAuthScreen will handle the actual provider selection internally
        // when this state is active, unless we provide custom buttons here to trigger
        // the reauth process manually if the library allows it via some callback.
        // Usually, the library shows the MethodPicker again.

        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(CoreUiR.string.action_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
