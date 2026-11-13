package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.WearPairing
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.PhonePairingCode
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.messageRes
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingStage
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel.PhoneWearPairingViewModel

/**
 * Phone side of the watch pairing (sdd-055). [args] is the route the deeplink opened. Leaving the
 * screen through [onClose] before the pairing ends cancels it, and the watch is told.
 */
@Composable
internal fun PhoneWearPairingScreen(
    viewModel: PhoneWearPairingViewModel,
    args: WearPairing,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(args.sessionId) {
        viewModel.start(args.sessionId, args.wearPublicKey, args.wearNodeId)
    }

    PhoneWearPairingContent(
        state = uiState,
        onConfirm = { viewModel.confirm(context) },
        onCancel = {
            viewModel.cancel()
            onClose()
        },
        onClose = onClose
    )
}

@Composable
private fun PhoneWearPairingContent(
    state: PhoneWearPairingUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(TAG_ROOT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state.stage) {
            PhoneWearPairingStage.CONNECTING -> Progress(
                icon = Icons.AutoMirrored.Filled.Login,
                body = stringResource(R.string.phone_pair_connecting)
            )

            PhoneWearPairingStage.SYNCING -> Progress(
                icon = Icons.AutoMirrored.Filled.Login,
                body = stringResource(R.string.phone_pair_syncing)
            )

            PhoneWearPairingStage.CODE -> CodeStep(state = state, onConfirm = onConfirm, onCancel = onCancel)

            PhoneWearPairingStage.DONE -> ResultStep(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.phone_pair_done),
                onClose = onClose,
                closeTag = TAG_CLOSE
            )

            PhoneWearPairingStage.ERROR -> ResultStep(
                icon = Icons.Default.ErrorOutline,
                title = stringResource((state.failure ?: PairingFailure.TIMEOUT).messageRes()),
                onClose = onClose,
                closeTag = TAG_CLOSE,
                titleTag = TAG_ERROR
            )
        }
    }
}

/** A ring around the icon while the pairing waits for the watch or syncs the account. */
@Composable
private fun Progress(icon: ImageVector, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(BADGE_SIZE), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = body, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CodeStep(state: PhoneWearPairingUiState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Badge(icon = Icons.AutoMirrored.Filled.Login)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.phone_pair_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        PhonePairingCode(
            code = state.code,
            tag = TAG_CODE,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.phone_pair_code_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (state.isPhoneConfirmed) {
            CircularProgressIndicator(modifier = Modifier.size(ICON_SIZE))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.phone_pair_waiting_watch), textAlign = TextAlign.Center)
        } else {
            CestouButton(
                text = stringResource(R.string.phone_pair_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().testTag(TAG_CONFIRM)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        CestouButton(
            text = stringResource(R.string.phone_pair_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().testTag(TAG_CANCEL)
        )
    }
}

@Composable
private fun ResultStep(
    icon: ImageVector,
    title: String,
    onClose: () -> Unit,
    closeTag: String,
    titleTag: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Badge(icon = icon)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = if (titleTag != null) Modifier.testTag(titleTag) else Modifier
        )
        Spacer(modifier = Modifier.height(24.dp))
        CestouButton(
            text = stringResource(R.string.phone_pair_close),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().testTag(closeTag)
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

private val BADGE_SIZE = 72.dp
private val BADGE_CORNER = 24.dp
private val ICON_SIZE = 36.dp

private const val TAG_ROOT = "phone_pair_root"
private const val TAG_CODE = "phone_pair_code"
private const val TAG_CONFIRM = "phone_pair_confirm"
private const val TAG_CANCEL = "phone_pair_cancel"
private const val TAG_CLOSE = "phone_pair_close"
private const val TAG_ERROR = "phone_pair_error"

@PreviewCestouScreens
@Composable
private fun PhoneWearPairingCodePreview() {
    CestouTheme {
        PhoneWearPairingContent(
            state = PhoneWearPairingUiState(stage = PhoneWearPairingStage.CODE, code = "BDFG HKLM"),
            onConfirm = {},
            onCancel = {},
            onClose = {}
        )
    }
}
