package br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent

internal data class WelcomeIntent(
    val onSignInSuccess: () -> Unit = {},
    val onSignInFailure: (Exception) -> Unit = {},
    val onLanguageSelected: (String) -> Unit = {}
)
