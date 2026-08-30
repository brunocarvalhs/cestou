package br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent

internal data class WelcomeIntent(
    val onSignInFailure: (Exception) -> Unit = {}
)
