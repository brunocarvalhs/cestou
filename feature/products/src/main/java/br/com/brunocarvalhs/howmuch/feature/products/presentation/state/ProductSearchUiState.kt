package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.Recipe

@Stable
internal data class ProductSearchUiState(
    val query: String = "",
    val results: List<Product> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipe: Recipe? = null,
    val searchMode: SearchMode = SearchMode.PRODUCT,
    val isSearching: Boolean = false,
    val errorMessage: String? = null
) {
    enum class SearchMode {
        PRODUCT, RECIPE
    }
}
