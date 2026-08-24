package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Product

@Stable
internal data class ProductSuggestionUiState(
    val suggestions: List<Product> = emptyList(),
    val selectedProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)
