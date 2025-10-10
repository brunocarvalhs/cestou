package br.com.brunocarvalhs.howmuch.app.modules.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart

data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyItems: List<ShoppingCart> = emptyList(),
)
