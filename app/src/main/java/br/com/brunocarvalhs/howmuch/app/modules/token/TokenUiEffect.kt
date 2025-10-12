package br.com.brunocarvalhs.howmuch.app.modules.token

sealed interface TokenUiEffect {
    data class ShowError(val message: String) : TokenUiEffect
    data class NavigateToAddProduct(val cartId: String?) : TokenUiEffect
    data class ShareCart(val shareText: String) : TokenUiEffect
}
