package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

sealed interface ShoppingCartUiIntent {
    data class RemoveItem(val productId: String) : ShoppingCartUiIntent
    data class UpdateQuantity(val productId: String, val quantity: Int) : ShoppingCartUiIntent
    object Retry : ShoppingCartUiIntent

    data class SetLimitCard(val limit: Long) : ShoppingCartUiIntent
}
