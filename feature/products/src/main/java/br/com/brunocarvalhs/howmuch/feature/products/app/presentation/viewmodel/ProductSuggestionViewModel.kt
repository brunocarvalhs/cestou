package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.GetProductSuggestionsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.commons.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.event.ProductSuggestionEvent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductSuggestionIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductSuggestionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel
internal class ProductSuggestionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductSuggestionsUseCase: GetProductSuggestionsUseCase,
    private val productSaveUseCase: ProductSaveUseCase
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(
        ProductPickerRoute.typeMap
    ).shopping

    private val _uiState = MutableStateFlow(ProductSuggestionUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<ProductSuggestionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(ProductSuggestionEvent.Error(error.message.orEmpty()))
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

    private fun addProduct(product: Product) {
        viewModelScope.launch {
            productSaveUseCase(product = product, shoppingId = shopping.id)
                .onSuccess {
                    _events.send(
                        ProductSuggestionEvent.ProductAdded
                    )
                }
                .onFailure {
                    _events.send(
                        ProductSuggestionEvent.Error(
                            it.message.orEmpty()
                        )
                    )
                }
        }
    }
}
