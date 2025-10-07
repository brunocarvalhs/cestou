package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import br.com.brunocarvalhs.domain.entities.Product

sealed interface ShoppingCartUiIntent {
    object AddProduct : ShoppingCartUiIntent
    data class SearchByToken(val token: String) : ShoppingCartUiIntent
    data class RemoveItem(val productId: String) : ShoppingCartUiIntent
    data class UpdateQuantity(val productId: String, val quantity: Int) : ShoppingCartUiIntent
    object Retry : ShoppingCartUiIntent
    data class FinalizePurchase(val market: String, val totalPrice: Long) : ShoppingCartUiIntent
    data class UpdateChecked(
        val product: Product,
        val price: Long,
        val isChecked: Boolean
    ) : ShoppingCartUiIntent
}
