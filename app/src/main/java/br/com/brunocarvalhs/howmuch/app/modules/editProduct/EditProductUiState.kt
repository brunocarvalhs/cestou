package br.com.brunocarvalhs.howmuch.app.modules.editProduct

data class EditProductUiState(
    val cartId: String = "",
    val productId: String = "",

    val isEditName: Boolean = false,
    val isEditPrice: Boolean = false,
    val isEditQuantity: Boolean = false,
    val name: String = "",
    val price: Long = 0L,
    val quantity: Int = 0,
    val isChecked: Boolean = false,

    val isLoading: Boolean = false,
)
