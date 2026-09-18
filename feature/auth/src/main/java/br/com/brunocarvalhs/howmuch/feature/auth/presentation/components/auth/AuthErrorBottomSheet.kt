package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.AuthErrorType

/**
 * Bottom sheet surfaced on [br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.WelcomeScreen]
 * whenever a sign-in attempt fails, so the user gets real feedback instead of silence (or a
 * confusing system dialog, e.g. Credential Manager's "No Google accounts available" for what was
 * actually a network outage — see [AuthErrorType]).
 *
 * There is no real "retry sign-in" hook exposed by [br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.WelcomeViewModel]:
 * the actual sign-in trigger lives inside FirebaseUI's `FirebaseAuthScreen` method-picker UI, not
 * here. So the primary action and the close (X) button both just dismiss the sheet — dismissing
 * *is* the retry path, since it lets the user tap the sign-in button again themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthErrorBottomSheet(
    errorType: AuthErrorType,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val (icon, titleRes, messageRes) = errorType.toContent()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.testTag("auth_error_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("auth_error_sheet_icon"),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auth_error_sheet_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("auth_error_sheet_message")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_error_sheet_action_button")
            ) {
                Text(stringResource(R.string.auth_error_action_try_again))
            }
        }
    }
}

private data class AuthErrorContent(
    val icon: ImageVector,
    val titleRes: Int,
    val messageRes: Int
)

private fun AuthErrorType.toContent(): AuthErrorContent = when (this) {
    AuthErrorType.NO_CONNECTIVITY -> AuthErrorContent(
        icon = Icons.Default.WifiOff,
        titleRes = R.string.auth_error_no_connectivity_title,
        messageRes = R.string.auth_error_no_connectivity_message
    )

    AuthErrorType.CONNECTION_FAILURE -> AuthErrorContent(
        icon = Icons.Default.CloudOff,
        titleRes = R.string.auth_error_connection_failure_title,
        messageRes = R.string.auth_error_connection_failure_message
    )

    AuthErrorType.STRUCTURAL -> AuthErrorContent(
        icon = Icons.Default.ErrorOutline,
        titleRes = R.string.auth_error_structural_title,
        messageRes = R.string.auth_error_structural_message
    )
}
