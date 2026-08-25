package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.intent

internal data class LinkWearIntent(
    val onCodeChange: (String) -> Unit = {},
    val onLinkClick: () -> Unit = {}
)
