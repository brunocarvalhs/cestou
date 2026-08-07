package br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent

internal data class NotificationSettingsIntent(
    val onUpdateNotificationSettings: (Boolean, String) -> Unit = { _, _ -> },
    val onBack: () -> Unit = {}
)
