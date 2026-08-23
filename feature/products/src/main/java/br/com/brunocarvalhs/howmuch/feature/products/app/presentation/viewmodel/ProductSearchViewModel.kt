package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.RecipeSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.commons.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductSearchIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductSearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
internal class ProductSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchUseCase: ProductSearchUseCase,
    private val recipeSearchUseCase: RecipeSearchUseCase,
    private val saveUseCase: ProductSaveUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(ProductSearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        analyticsTracker.trackScreenView(screenName = "product_search", screenClass = "ProductSearchViewModel")
    }

    val intent = ProductSearchIntent(
        onQueryChange = { onQueryChange(it) },
        onProductSelected = { onProductSelected(it) },
        onSearchModeChange = { mode -> 
            _uiState.update { it.copy(searchMode = mode, query = "", results = emptyList(), recipes = emptyList()) }
        },
        onRecipeSelected = { recipe -> _uiState.update { it.copy(selectedRecipe = recipe) } },
        onClearRecipeSelection = { _uiState.update { it.copy(selectedRecipe = null) } },
        onAddRecipeIngredients = { recipe -> onAddRecipeIngredients(recipe) }
    )

    private fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.length >= MIN_QUERY_LENGTH) {
            search()
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
    }

    private fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val query = _uiState.value.query
            if (_uiState.value.searchMode == ProductSearchUiState.SearchMode.PRODUCT) {
                searchUseCase(query)
                    .onSuccess { results ->
                        _uiState.update { it.copy(results = results, isSearching = false) }
                        trackSearchPerformed(query = query, mode = "product", resultCount = results.size)
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, errorMessage = e.message) }
                    }
            } else {
                recipeSearchUseCase(query)
                    .onSuccess { results ->
                        _uiState.update { it.copy(recipes = results, isSearching = false) }
                        trackSearchPerformed(query = query, mode = "recipe", resultCount = results.size)
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, errorMessage = e.message) }
                    }
            }
        }
    }

    private fun trackSearchPerformed(query: String, mode: String, resultCount: Int) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.PRODUCT_SEARCH_PERFORMED,
            mapOf(
                AnalyticsParams.SEARCH_MODE to mode,
                AnalyticsParams.QUERY_LENGTH to query.length,
                AnalyticsParams.RESULT_COUNT to resultCount
            )
        )
    }

    private fun onProductSelected(product: Product) {
        viewModelScope.launch {
            saveUseCase(product = product, shoppingId = shopping.id)
            analyticsTracker.trackEvent(
                AnalyticsEvents.PRODUCT_SELECTED,
                mapOf(AnalyticsParams.SHOPPING_ID to shopping.id, AnalyticsParams.PRODUCT_ID to product.id)
            )
        }
    }

    private fun onAddRecipeIngredients(recipe: Recipe) {
        viewModelScope.launch {
            recipe.ingredients.forEach { product ->
                saveUseCase(product = product.copy(id = UUID.randomUUID().toString()), shoppingId = shopping.id)
            }
            _uiState.update { it.copy(selectedRecipe = null, query = "", recipes = emptyList()) }
        }
    }
}
