package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

internal data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
