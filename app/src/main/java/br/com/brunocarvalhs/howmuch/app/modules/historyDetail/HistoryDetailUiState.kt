package br.com.brunocarvalhs.howmuch.app.modules.historyDetail

import br.com.brunocarvalhs.domain.entities.ShoppingCart

data class HistoryDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cart: ShoppingCart? = null,
    val isDeleting: Boolean = false
)
