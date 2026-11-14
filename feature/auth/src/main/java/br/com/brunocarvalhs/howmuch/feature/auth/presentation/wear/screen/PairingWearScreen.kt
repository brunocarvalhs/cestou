package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.WearPairingCode
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.WearScrollableScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.messageRes
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingStage
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel.PairingViewModel

/** Watch pairing (sdd-055): starts a session when shown, and leaves to the home once linked. */
@Composable
internal fun PairingWearScreen(
    viewModel: PairingViewModel,
    navigator: Navigator
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(uiState.isLinked) {
        if (uiState.isLinked) {
            navigator.navigate(ShoppingList::class.java.name) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    PairingWearContent(
        state = uiState,
        onConfirm = viewModel::confirm,
        onRetry = viewModel::start,
        onExit = {
            viewModel.cancel()
            navigator.goBack()
        }
    )
}

@Composable
private fun PairingWearContent(
    state: PairingUiState,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    WearScrollableScreen(modifier = Modifier.testTag(TAG_ROOT)) {
        when (state.stage) {
            PairingStage.IDLE, PairingStage.CONNECTING -> Progress(
                icon = Icons.AutoMirrored.Filled.Login,
                body = stringResource(R.string.wear_pair_connecting)
            )

            PairingStage.SYNCING -> Progress(
                icon = Icons.AutoMirrored.Filled.Login,
                body = stringResource(R.string.wear_pair_syncing)
            )

            PairingStage.CODE -> CodeStep(code = state.code, onConfirm = onConfirm)

            PairingStage.ERROR -> ErrorStep(
                failure = state.failure ?: PairingFailure.TIMEOUT,
                onRetry = onRetry,
                onExit = onExit
            )
        }
    }
}

/** A ring around the icon while the phone is reached or the account syncs. */
@Composable
private fun Progress(icon: ImageVector, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(BADGE_SIZE), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** The phone shows the same code: the user checks both screens match, then taps "Parear". */
@Composable
private fun CodeStep(code: String, onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Badge(icon = Icons.AutoMirrored.Filled.Login)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wear_pair_code_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        WearPairingCode(
            code = code,
            tag = TAG_CODE,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.wear_pair_code_body),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        CestouButton(
            text = stringResource(R.string.wear_pair_confirm),
            onClick = onConfirm,
            modifier = Modifier.testTag(TAG_CONFIRM)
        )
    }
}

@Composable
private fun ErrorStep(failure: PairingFailure, onRetry: () -> Unit, onExit: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Badge(icon = Icons.Default.ErrorOutline)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wear_pair_error_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(failure.messageRes()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag(TAG_ERROR)
        )
        Spacer(modifier = Modifier.height(8.dp))
        CestouButton(
            text = stringResource(R.string.wear_pair_retry),
            onClick = onRetry,
            modifier = Modifier.testTag(TAG_RETRY)
        )
        Spacer(modifier = Modifier.height(4.dp))
        CestouButton(
            text = stringResource(R.string.wear_pair_exit),
            onClick = onExit,
            modifier = Modifier.testTag(TAG_EXIT)
        )
    }
}

/** The rounded tile with the icon that opens each step, as in the reference flow. */
@Composable
private fun Badge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(BADGE_SIZE)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(BADGE_CORNER)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(ICON_SIZE)
        )
    }
}

private val BADGE_SIZE = 56.dp
private val BADGE_CORNER = 20.dp
private val ICON_SIZE = 28.dp

private const val TAG_ROOT = "wear_pair_root"
private const val TAG_CODE = "wear_pair_code"
private const val TAG_CONFIRM = "wear_pair_confirm"
private const val TAG_ERROR = "wear_pair_error"
private const val TAG_RETRY = "wear_pair_retry"
private const val TAG_EXIT = "wear_pair_exit"

@WearPreviewDevices
@Composable
private fun PairingWearCodePreview() {
    MaterialTheme {
        PairingWearContent(
            state = PairingUiState(stage = PairingStage.CODE, code = "BDFG HKLM"),
            onConfirm = {},
            onRetry = {},
            onExit = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun PairingWearErrorPreview() {
    MaterialTheme {
        PairingWearContent(
            state = PairingUiState(stage = PairingStage.ERROR, failure = PairingFailure.PHONE_UNREACHABLE),
            onConfirm = {},
            onRetry = {},
            onExit = {}
        )
    }
}
