package br.com.brunocarvalhs.howmuch.app.modules.historyDetail

import android.content.Context
import br.com.brunocarvalhs.domain.entities.ShoppingCart

sealed interface HistoryDetailUiIntent {
    data object FetchCart : HistoryDetailUiIntent
    data class CreateListFromHistory(val cart: ShoppingCart) : HistoryDetailUiIntent
    data class ShareCart(val context: Context, val cart: ShoppingCart) : HistoryDetailUiIntent
    data class Delete(val cart: ShoppingCart) : HistoryDetailUiIntent
}
