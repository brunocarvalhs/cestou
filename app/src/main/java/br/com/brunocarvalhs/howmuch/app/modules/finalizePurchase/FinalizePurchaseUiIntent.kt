package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

sealed interface FinalizePurchaseUiIntent {
    data class FinalizePurchase(val cartId: String?, val name: String, val price: Long) :
        FinalizePurchaseUiIntent
}