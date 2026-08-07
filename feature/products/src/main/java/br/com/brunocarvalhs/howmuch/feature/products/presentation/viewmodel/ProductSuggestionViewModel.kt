package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.GetProductSuggestionsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.presentation.event.ProductSuggestionEvent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductSuggestionIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductSuggestionUiState
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product as ProductEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class ProductSuggestionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductSuggestionsUseCase: GetProductSuggestionsUseCase,
    private val productSaveUseCase: ProductSaveUseCase
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(ProductSuggestionUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProductSuggestionEvent>()
    val events = _events.asSharedFlow()

    val intent = ProductSuggestionIntent(
        onAddProduct = { product -> addProduct(product) },
        onSearchProduct = { query -> searchProduct(query) }
    )

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            getProductSuggestionsUseCase(shopping.id)
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message)
                    }
                }
                .collect { products ->
                    _uiState.update {
                        it.copy(
                            suggestions = products,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun searchProduct(query: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(searchQuery = query)
            }
        }
    }

    private fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            productSaveUseCase(product = product, shoppingId = shopping.id)
                .onSuccess {
                    _events.emit(
                        ProductSuggestionEvent.ProductAdded
                    )
                }
                .onFailure {
                    _events.emit(
                        ProductSuggestionEvent.Error(
                            it.message.orEmpty()
                        )
                    )
                }
        }
    }
}
