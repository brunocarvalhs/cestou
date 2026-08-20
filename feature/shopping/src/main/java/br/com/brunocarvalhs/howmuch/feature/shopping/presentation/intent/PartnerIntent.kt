package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

internal data class PartnerIntent(
    val onLinkPartner: (String) -> Unit = {},
    val onUnlinkPartner: () -> Unit = {}
)
