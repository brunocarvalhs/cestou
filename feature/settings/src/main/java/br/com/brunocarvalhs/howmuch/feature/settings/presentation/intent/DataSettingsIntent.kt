package br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent

internal data class DataSettingsIntent(
    val onClearCache: () -> Unit = {},
    val onDeleteAllData: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
    val onReauthConfirmed: () -> Unit = {},
    val onReauthDismissed: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)
