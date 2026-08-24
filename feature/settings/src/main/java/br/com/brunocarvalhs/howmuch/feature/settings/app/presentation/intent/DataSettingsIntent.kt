package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent

internal data class DataSettingsIntent(
    val onClearCache: () -> Unit = {},
    val onDeleteAllData: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)
