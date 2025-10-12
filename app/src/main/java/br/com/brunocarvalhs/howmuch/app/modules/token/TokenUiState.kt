package br.com.brunocarvalhs.howmuch.app.modules.token

data class TokenUiState(
    val isLoading: Boolean = false,
    val token: String? = null,
    val cartId: String? = null,
)
