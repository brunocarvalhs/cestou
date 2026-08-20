package br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent

internal data class LoginIntent(
    val onGoogleLogin: () -> Unit = {},
    val onAppleLogin: () -> Unit = {},
    val onBack: () -> Unit = {}
)
