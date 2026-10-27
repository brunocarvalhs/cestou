package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.EditItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.EditItemUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EditItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCase: ProductsUseCase,
    private val authService: AuthService
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<EditItemRoute>(EditItemRoute.typeMap).shoppingId
    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(EditItemUiState())
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onSaveEdit(product: Product, price: Double?, quantity: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authService.getOrCreateUserId().id
            useCase.update(
                shoppingId = shoppingId,
                product = product.copy(price = price, quantity = quantity)
                    .withActivity(ProductActivity.Action.EDITED, userId),
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _navigator?.goBack()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.DynamicString(
                            throwable.message ?: "Failed to save changes"
                        )
                    )
                }
            }
        }
    }
}
