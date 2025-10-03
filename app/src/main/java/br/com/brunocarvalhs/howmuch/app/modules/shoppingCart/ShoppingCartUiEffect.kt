package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

sealed interface ShoppingCartUiEffect {
    data class ShowError(val message: String) : ShoppingCartUiEffect
    data class NavigateToAddProduct(val cartId: String?) : ShoppingCartUiEffect
    data class ShareCart(val shareText: String) : ShoppingCartUiEffect
}
