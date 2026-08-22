package br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent

internal data class ProfileIntent(
    val onNavigate: (Any) -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onDisconnectPartner: () -> Unit = {}
)
