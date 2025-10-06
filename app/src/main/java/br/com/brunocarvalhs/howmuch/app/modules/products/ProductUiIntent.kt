package br.com.brunocarvalhs.howmuch.app.modules.products

sealed class ProductUiIntent {
    data class LoadShoppingCart(val cartId: String?) : ProductUiIntent()
    data class AddProductToCart(
        val name: String,
        val price: Long,
        val quantity: Int
    ) : ProductUiIntent()

    data class AddProductToList(
        val name: String,
        val quantity: Int
    ) : ProductUiIntent()
}
