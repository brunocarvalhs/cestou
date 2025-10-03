package br.com.brunocarvalhs.howmuch.app.modules.products

data class ProductUiState(
    val isLoading: Boolean = false,
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productQuantity: Int = 1,
    val isProductAdded: Boolean = false,
)
