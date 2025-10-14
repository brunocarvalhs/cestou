package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import br.com.brunocarvalhs.domain.entities.Product

sealed interface ShoppingCartUiIntent {
    data class RemoveItem(val product: Product) : ShoppingCartUiIntent
    data class UpdateQuantity(val product: Product, val quantity: Int) : ShoppingCartUiIntent
    object Retry : ShoppingCartUiIntent

    data class SetLimitCard(val limit: Long) : ShoppingCartUiIntent
}
