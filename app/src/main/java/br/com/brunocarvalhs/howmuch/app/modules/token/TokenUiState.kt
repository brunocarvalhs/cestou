package br.com.brunocarvalhs.howmuch.app.modules.token

sealed class TokenUiState {
    data object Idle : TokenUiState()
    data class Success(val cartId: String) : TokenUiState()
    data class Error(val message: String) : TokenUiState()
    data object Loading : TokenUiState()
}
