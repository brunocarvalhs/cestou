package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.intent

internal data class WelcomeIntent(
    val onStart: () -> Unit = {}
)
