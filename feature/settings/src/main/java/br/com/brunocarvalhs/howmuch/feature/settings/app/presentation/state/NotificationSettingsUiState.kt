package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "18:00"
)
