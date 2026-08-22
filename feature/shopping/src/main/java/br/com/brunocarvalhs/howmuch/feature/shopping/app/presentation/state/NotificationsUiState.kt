package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state

internal data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
)

internal data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

internal enum class NotificationType {
    ACTION, REMINDER, FEATURE
}
