package br.com.brunocarvalhs.howmuch.app.modules.products

sealed class ProductUiIntent {
    data class LoadShoppingCart(val cartId: String?) : ProductUiIntent()
    data class AddProduct(
        val name: String,
        val price: Long,
        val quantity: Int
    ) : ProductUiIntent()
}
