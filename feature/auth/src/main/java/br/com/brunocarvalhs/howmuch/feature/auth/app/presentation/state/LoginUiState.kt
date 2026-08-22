package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state

internal data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
