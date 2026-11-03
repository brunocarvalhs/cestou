package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationSectionKey

@Stable
internal data class NotificationsUiState(
    val sections: StableList<NotificationSection> = StableList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    val hasUnread: Boolean = false,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    /** Sender id -> profile, filled lazily; a missing entry renders the type icon instead of an avatar. */
    val senderProfiles: Map<String, UserProfile> = emptyMap()
)

@Stable
internal data class NotificationSection(
    val key: NotificationSectionKey,
    val items: StableList<NotificationItem>
)

@Stable
internal data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val relativeTime: String,
    val type: NotificationType,
    val isRead: Boolean,
    val shoppingId: String?,
    val senderId: String?
)

internal enum class NotificationType {
    LIST_JOINED, LIST_FINISHED, CAMPAIGN, OTHER
}
