package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

sealed interface ShoppingCartUiEffect {
    data class ShowError(val message: String) : ShoppingCartUiEffect
}
