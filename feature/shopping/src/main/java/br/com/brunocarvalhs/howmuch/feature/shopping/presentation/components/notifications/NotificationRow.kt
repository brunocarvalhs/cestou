package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.components.UserAvatar
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType

private val AVATAR_SIZE = 40.dp
private val UNREAD_DOT_SIZE = 8.dp

internal fun notificationItemTag(id: String) = "notification_item_$id"
internal fun notificationUnreadDotTag(id: String) = "notification_unread_dot_$id"

/** [onClick] receives the id so the caller can pass one stable lambda for every row. */
@Composable
internal fun NotificationRow(
    item: NotificationItem,
    senderProfile: UserProfile?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CestouCard(
        modifier = modifier.fillMaxWidth().testTag(notificationItemTag(item.id)),
        onClick = { onClick(item.id) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (senderProfile != null) {
                UserAvatar(profile = senderProfile, size = AVATAR_SIZE)
            } else {
                TypeIcon(item.type)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!item.isRead) {
                val unread = stringResource(R.string.notifications_unread_indicator)
                Box(
                    modifier = Modifier
                        .size(UNREAD_DOT_SIZE)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .semantics { contentDescription = unread }
                        .testTag(notificationUnreadDotTag(item.id))
                )
            }
        }
    }
}

@Composable
private fun TypeIcon(type: NotificationType) {
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (type) {
                NotificationType.LIST_JOINED -> Icons.Default.PersonAdd
                NotificationType.LIST_FINISHED -> Icons.Default.CheckCircle
                NotificationType.CAMPAIGN -> Icons.Default.Campaign
                NotificationType.OTHER -> Icons.Default.Notifications
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationRowPreview() {
    CestouTheme {
        NotificationRow(
            item = NotificationItem(
                id = "1",
                title = "New member",
                description = "Ana joined \"Weekly groceries\"",
                relativeTime = "2 min ago",
                type = NotificationType.LIST_JOINED,
                isRead = false,
                shoppingId = "s1",
                senderId = "u2"
            ),
            senderProfile = UserProfile(id = "u2", name = "Ana Nunes"),
            onClick = {}
        )
    }
}
