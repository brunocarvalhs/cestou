package br.com.brunocarvalhs.howmuch.app.modules.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.navArgs
import androidx.navigation.toRoute
import br.com.brunocarvalhs.data.model.ProductModel
import br.com.brunocarvalhs.domain.usecases.product.AddProductUseCase
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.ProductGraphRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addProductUseCase: AddProductUseCase,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ProductGraphRoute>()
    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductUiEffect>()
    val uiEffect: SharedFlow<ProductUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: ProductUiIntent) {
        when (intent) {
            is ProductUiIntent.AddProductToCart -> addProduct(
                name = intent.name,
                price = intent.price,
                quantity = intent.quantity
            )

            is ProductUiIntent.AddProductToList -> addProduct(
                name = intent.name,
                quantity = intent.quantity,
            )
        }
    }

    private fun addProduct(name: String, price: Long? = null, quantity: Int) =
        viewModelScope.launch {
            val product = ProductModel(
                name = name,
                price = price,
                quantity = quantity,
                isChecked = price != null
            )
            addProductUseCase(args.cartId, product)
                .onSuccess {
                    _uiEffect.emit(ProductUiEffect.ProductAdded)
                }
                .onFailure {
                    _uiEffect.emit(ProductUiEffect.ShowError(it.message ?: "Unknown error"))
                }
        }
}
