package br.com.brunocarvalhs.howmuch.feature.products.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductSearchUiState

internal data class ProductSearchIntent(
    val onQueryChange: (String) -> Unit = {},
    val onProductSelected: (Product) -> Unit = {},
    val onSearchModeChange: (ProductSearchUiState.SearchMode) -> Unit = {},
    val onRecipeSelected: (Recipe) -> Unit = {},
    val onClearRecipeSelection: () -> Unit = {},
    val onAddRecipeIngredients: (Recipe) -> Unit = {},
    val onErrorShown: () -> Unit = {}
)
