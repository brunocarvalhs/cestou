package br.com.brunocarvalhs.howmuch.app.modules.products

sealed class ProductUiEffect {
    data class ShowError(val message: String) : ProductUiEffect()
    data object ProductAdded : ProductUiEffect()
}
