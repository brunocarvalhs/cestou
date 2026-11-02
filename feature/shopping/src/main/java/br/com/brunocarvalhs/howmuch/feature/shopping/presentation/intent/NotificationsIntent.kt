package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter

internal data class NotificationsIntent(
    val onNotificationClick: (String) -> Unit = {},
    val onFilter: (NotificationFilter) -> Unit = {},
    val onMarkAllRead: () -> Unit = {},
    val onErrorShown: () -> Unit = {},
    /** Re-renders the relative times ("5 min ago"), which are otherwise frozen at the last emission. */
    val onRefreshTimes: () -> Unit = {},
    val onBack: () -> Unit = {}
)
