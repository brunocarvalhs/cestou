package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.entity.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.RecipeSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductSearchIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductSearchUiState
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
    private val saveUseCase: ProductSaveUseCase
) : ViewModel() {
    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(ProductSearchUiState())
    val uiState = _uiState.asStateFlow()

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
            if (_uiState.value.searchMode == ProductSearchUiState.SearchMode.PRODUCT) {
                searchUseCase(_uiState.value.query)
                    .onSuccess { results ->
                        _uiState.update { it.copy(results = results, isSearching = false) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, errorMessage = e.message) }
                    }
            } else {
                recipeSearchUseCase(_uiState.value.query)
                    .onSuccess { results ->
                        _uiState.update { it.copy(recipes = results, isSearching = false) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, errorMessage = e.message) }
                    }
            }
        }
    }

    private fun onProductSelected(product: Product) {
        viewModelScope.launch {
            saveUseCase(product = product, shoppingId = shopping.id)
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
