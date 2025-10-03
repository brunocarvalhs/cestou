package br.com.brunocarvalhs.howmuch.app.modules.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart

sealed interface HistoryUiIntent {
    data class DeleteSelected(val list: List<ShoppingCart>) : HistoryUiIntent
    object Retry : HistoryUiIntent
    object ClearHistory : HistoryUiIntent
}
