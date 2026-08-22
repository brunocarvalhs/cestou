package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent

internal data class ProfileIntent(
    val onNavigate: (Any) -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onDisconnectPartner: () -> Unit = {}
)
