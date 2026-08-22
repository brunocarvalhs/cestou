package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent

internal data class PartnerIntent(
    val onLinkPartner: (String) -> Unit = {},
    val onUnlinkPartner: () -> Unit = {}
)
