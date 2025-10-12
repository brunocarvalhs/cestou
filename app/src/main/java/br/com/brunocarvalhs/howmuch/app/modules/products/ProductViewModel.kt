package br.com.brunocarvalhs.howmuch.app.modules.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.data.model.ProductModel
import br.com.brunocarvalhs.domain.usecases.product.AddProductUseCase
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
    private val addProductUseCase: AddProductUseCase,
) : ViewModel() {

    private var cartId: String? = null

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

            is ProductUiIntent.LoadShoppingCart -> intent.cartId?.let { setCartId(it) }

            is ProductUiIntent.AddProductToList -> addProduct(
                name = intent.name,
                quantity = intent.quantity,
            )
        }
    }

    private fun setCartId(id: String) {
        cartId = id
    }

    private fun addProduct(name: String, price: Long? = null, quantity: Int) = viewModelScope.launch {
        cartId?.let { cartId ->
            val product = ProductModel(
                name = name,
                price = price,
                quantity = quantity,
                isChecked = price != null
            )
            addProductUseCase(cartId, product)
                .onSuccess {
                    _uiEffect.emit(ProductUiEffect.ProductAdded)
                }
                .onFailure {
                    _uiEffect.emit(ProductUiEffect.ShowError(it.message ?: "Unknown error"))
                }
        } ?: _uiEffect.emit(ProductUiEffect.ShowError("Cart ID is not set"))
    }
}
