package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.event

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ProductSuggestionEvent {
    data object ProductAdded : ProductSuggestionEvent
    data class Error(val message: String) : ProductSuggestionEvent
}
