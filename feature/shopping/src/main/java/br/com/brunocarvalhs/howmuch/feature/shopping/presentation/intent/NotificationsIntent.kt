package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

internal data class NotificationsIntent(
    val onNotificationClick: (String) -> Unit = {},
    val onBack: () -> Unit = {}
)
