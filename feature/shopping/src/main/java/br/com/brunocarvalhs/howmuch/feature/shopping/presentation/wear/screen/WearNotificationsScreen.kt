package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearNotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearNotificationsUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearNotificationsViewModel

/** Tags para os testes de UI (AGENTS §5: sem asserts de texto). */
internal const val TAG_NOTIFICATIONS_MARK_READ = "wear_notifications_mark_read"
internal const val TAG_NOTIFICATIONS_EMPTY = "wear_notifications_empty"

private val RowRadius = 16.dp

/** Notificações: as mais recentes da conta, com "Marcar lidas" quando há não lidas. */
@Composable
internal fun WearNotificationsScreen(viewModel: WearNotificationsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WearNotificationsContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun WearNotificationsContent(
    state: WearNotificationsUiState,
    onIntent: (WearNotificationsIntent) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val items = state.items.items

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_notifications_title))
                }
            }

            if (state.unreadCount > 0) {
                item {
                    Button(
                        onClick = { onIntent(WearNotificationsIntent.MarkAllRead) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .testTag(TAG_NOTIFICATIONS_MARK_READ),
                        colors = wearCardColors(),
                        transformation = SurfaceTransformation(spec)
                    ) {
                        Text(stringResource(R.string.shopping_wear_notifications_mark_read))
                    }
                }
            }

            items(items.size, key = { items[it].id }) { index ->
                WearNotificationRow(
                    notification = items[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                )
            }

            if (!state.isLoading && (state.signedOut || items.isEmpty())) {
                item {
                    Text(
                        text = stringResource(R.string.shopping_wear_notifications_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(TAG_NOTIFICATIONS_EMPTY)
                    )
                }
            }
        }
    }
}

/** Uma notificação: título em destaque quando não lida, mensagem em duas linhas no máximo. */
@Composable
private fun WearNotificationRow(notification: Notification, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RowRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = notification.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (notification.isRead) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Text(
            text = notification.message,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
