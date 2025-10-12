package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

data class FinalizePurchaseUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
