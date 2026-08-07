package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

internal data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "18:00"
)
