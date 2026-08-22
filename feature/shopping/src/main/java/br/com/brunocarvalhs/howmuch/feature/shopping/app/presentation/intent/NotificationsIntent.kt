package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent

internal data class NotificationsIntent(
    val onNotificationClick: (String) -> Unit = {},
    val onBack: () -> Unit = {}
)
