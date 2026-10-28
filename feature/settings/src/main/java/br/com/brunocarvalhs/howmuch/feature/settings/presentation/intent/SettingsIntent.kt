package br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent

data class SettingsIntent(
    val onNavigate: (Any) -> Unit = {},
    val onBack: () -> Unit = {},
    val onSendEmail: (String) -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onUpdateLanguage: (String) -> Unit = {},
    val onUpdateCurrency: (String) -> Unit = {},
    val onToggleSection: (String) -> Unit = {}
)
