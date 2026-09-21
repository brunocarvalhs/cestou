package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.wear.ShoppingDetail
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.ShoppingDetailIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ShoppingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingGetByIdUseCase: ShoppingGetByIdUseCase,
    private val productsUseCase: ProductsUseCase
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<ShoppingDetail>().shoppingId
    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(ShoppingDetailUiState())
    val uiState: StateFlow<ShoppingDetailUiState> = _uiState.asStateFlow()

    val intent = ShoppingDetailIntent(
        onBack = { _navigator?.goBack() }
    )

    init {
        loadDetails()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            shoppingGetByIdUseCase(shoppingId).onSuccess { shopping ->
                productsUseCase(shoppingId).collect { products ->
                    val totalSpent = products.sumOf { it.total }
                    val budget = shopping.budget ?: 0.0
                    _uiState.update {
                        it.copy(
                            title = shopping.title,
                            budget = budget,
                            totalSpent = totalSpent,
                            balance = budget - totalSpent,
                            items = StableList(products),
                            isLoading = false
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
