package br.com.brunocarvalhs.howmuch.app.modules.editProduct

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.data.model.ProductModel
import br.com.brunocarvalhs.domain.usecases.product.EditProductUseCase
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.EditProductRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val editProductUseCase: EditProductUseCase
) : ViewModel() {

    private val args = savedStateHandle.toRoute<EditProductRoute>()

    private val _uiState = MutableStateFlow(
        EditProductUiState(
            cartId = args.cartId,
            productId = args.productId,
            isEditName = args.isEditName,
            isEditPrice = args.isEditPrice,
            isEditQuantity = args.isEditQuantity,
            name = args.name ?: "",
            price = args.price ?: 0L,
            quantity = args.quantity,
            isChecked = args.isChecked
        )
    )
    val uiState: StateFlow<EditProductUiState> = _uiState.asStateFlow()

    fun onIntent(intent: EditProductUiIntent) = when (intent) {
        is EditProductUiIntent.Save -> update(
            name = intent.name,
            quantity = intent.quantity,
            price = intent.price
        )
    }

    private fun update(name: String? = null, price: Long? = null, quantity: Int? = null) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        editProductUseCase.invoke(
            cartId = _uiState.value.cartId,
            product = ProductModel(
                id = _uiState.value.productId,
                name = name ?: _uiState.value.name,
                price = price ?: _uiState.value.price,
                quantity = quantity ?: _uiState.value.quantity,
                isChecked = _uiState.value.isChecked
            )
        )
        _uiState.value = _uiState.value.copy(isLoading = true)
    }
}
