package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationSectionKey
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.NotificationFilterChips
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.NotificationRow
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.NotificationsEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.NotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationSection
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationsUiState
import kotlinx.coroutines.delay

private const val TIME_REFRESH_INTERVAL_MS = 60_000L

internal const val NOTIFICATIONS_LIST_TAG = "notifications_list"
internal const val NOTIFICATIONS_LOADING_TAG = "notifications_loading"
internal const val NOTIFICATIONS_MARK_ALL_READ_TAG = "notifications_mark_all_read"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsScreen(
    state: NotificationsUiState,
    intent: NotificationsIntent
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                intent.onRefreshTimes()
                delay(TIME_REFRESH_INTERVAL_MS)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(message = it.asString(context))
            intent.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shopping_management_notifications_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = intent.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_back
                            )
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = intent.onMarkAllRead,
                        enabled = state.hasUnread,
                        modifier = Modifier.testTag(NOTIFICATIONS_MARK_ALL_READ_TAG)
                    ) {
                        Text(text = stringResource(R.string.notifications_mark_all_read))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            NotificationFilterChips(
                selected = state.filter,
                onSelect = intent.onFilter,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            NotificationsContent(state = state, onNotificationClick = intent.onNotificationClick)
        }
    }
}

@Composable
private fun NotificationsContent(
    state: NotificationsUiState,
    onNotificationClick: (String) -> Unit
) {
    when {
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize().testTag(NOTIFICATIONS_LOADING_TAG),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        state.sections.isEmpty() -> NotificationsEmptyState(isFiltered = state.filter != NotificationFilter.ALL)

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(NOTIFICATIONS_LIST_TAG),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.sections.forEach { section ->
                item(key = "header_${section.key.name}", contentType = "header") {
                    Text(
                        text = stringResource(section.key.title()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(section.items, key = { it.id }, contentType = { "notification" }) { item ->
                    NotificationRow(
                        item = item,
                        senderProfile = item.senderId?.let { state.senderProfiles[it] },
                        onClick = onNotificationClick
                    )
                }
            }
        }
    }
}

private fun NotificationSectionKey.title() = when (this) {
    NotificationSectionKey.TODAY -> R.string.notifications_section_today
    NotificationSectionKey.YESTERDAY -> R.string.notifications_section_yesterday
    NotificationSectionKey.EARLIER -> R.string.notifications_section_earlier
}

@PreviewCestouScreens
@Composable
private fun NotificationsPreview() {
    CestouTheme {
        NotificationsScreen(
            state = NotificationsUiState(
                isLoading = false,
                hasUnread = true,
                sections = StableList(
                    listOf(
                        NotificationSection(
                            NotificationSectionKey.TODAY,
                            StableList(
                                listOf(
                                    NotificationItem(
                                        "1",
                                        "New member",
                                        "Ana joined \"Weekly groceries\"",
                                        "2 min ago",
                                        NotificationType.LIST_JOINED,
                                        false,
                                        "s1",
                                        "u2"
                                    )
                                )
                            )
                        ),
                        NotificationSection(
                            NotificationSectionKey.YESTERDAY,
                            StableList(
                                listOf(
                                    NotificationItem(
                                        "2",
                                        "List finished",
                                        "Lucas finished \"Family\"",
                                        "1 d ago",
                                        NotificationType.LIST_FINISHED,
                                        true,
                                        "s2",
                                        null
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            intent = NotificationsIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun NotificationsEmptyPreview() {
    CestouTheme {
        NotificationsScreen(state = NotificationsUiState(isLoading = false), intent = NotificationsIntent())
    }
}
