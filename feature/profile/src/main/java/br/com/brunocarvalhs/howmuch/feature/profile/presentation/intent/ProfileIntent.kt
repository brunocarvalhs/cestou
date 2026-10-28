package br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent

internal data class ProfileIntent(
    val onSignOut: () -> Unit = {},
    val onLinkWearDevice: () -> Unit = {},
    val onLinkMobileDevice: () -> Unit = {},
    val onManageSubscription: () -> Unit = {}
)
