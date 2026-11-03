package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.shopping.R

internal const val NOTIFICATIONS_BELL_TAG = "notifications_bell"
internal const val NOTIFICATIONS_BADGE_TAG = "notifications_badge"
internal const val NOTIFICATIONS_BADGE_COUNT_TAG = "notifications_badge_count"
internal const val NOTIFICATIONS_BADGE_OVERFLOW_TAG = "notifications_badge_overflow"

/** Counts above this render as "9+": the badge is a nudge, not a counter. */
internal const val NOTIFICATIONS_BADGE_MAX = 9

@Composable
internal fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = if (unreadCount > 0) {
        pluralStringResource(R.plurals.shopping_notifications_bell_unread, unreadCount, unreadCount)
    } else {
        stringResource(R.string.shopping_notifications_content_description)
    }
    IconButton(onClick = onClick, modifier = modifier.testTag(NOTIFICATIONS_BELL_TAG)) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(modifier = Modifier.testTag(NOTIFICATIONS_BADGE_TAG).clearAndSetSemantics { }) {
                        Text(
                            text = badgeLabel(unreadCount),
                            modifier = Modifier.testTag(
                                if (unreadCount > NOTIFICATIONS_BADGE_MAX) {
                                    NOTIFICATIONS_BADGE_OVERFLOW_TAG
                                } else {
                                    NOTIFICATIONS_BADGE_COUNT_TAG
                                }
                            )
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun badgeLabel(unreadCount: Int): String =
    if (unreadCount > NOTIFICATIONS_BADGE_MAX) {
        stringResource(R.string.notifications_badge_overflow, NOTIFICATIONS_BADGE_MAX)
    } else {
        unreadCount.toString()
    }

@Preview(showBackground = true)
@Composable
private fun NotificationBellButtonPreview() {
    CestouTheme { NotificationBellButton(unreadCount = 12, onClick = {}) }
}
