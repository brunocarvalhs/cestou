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
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ConfirmItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.ConfirmItemUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ConfirmItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCase: ProductsUseCase,
    private val authService: AuthService
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<ConfirmItemRoute>(ConfirmItemRoute.typeMap).shoppingId
    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(ConfirmItemUiState())
    val uiState: StateFlow<ConfirmItemUiState> = _uiState.asStateFlow()

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onConfirmPurchased(product: Product, price: Double?, quantity: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authService.getOrCreateUserId().id
            useCase.update(
                shoppingId = shoppingId,
                product = product.copy(
                    isPurchased = true,
                    price = price,
                    quantity = quantity
                ).withActivity(ProductActivity.Action.PURCHASED, userId),
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _navigator?.goBack()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.DynamicString(
                            throwable.message ?: "Failed to confirm purchase"
                        )
                    )
                }
            }
        }
    }
}
