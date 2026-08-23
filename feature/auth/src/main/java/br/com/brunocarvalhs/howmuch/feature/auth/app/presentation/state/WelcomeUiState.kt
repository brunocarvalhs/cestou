package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class WelcomeUiState(
    val version: String = "1.2.0"
)
