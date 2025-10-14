package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

sealed interface FinalizePurchaseUiState {
    data object Idle : FinalizePurchaseUiState
    data object Loading : FinalizePurchaseUiState
    data class Error(val message: String) : FinalizePurchaseUiState
    data class Success(val message: String) : FinalizePurchaseUiState
}
