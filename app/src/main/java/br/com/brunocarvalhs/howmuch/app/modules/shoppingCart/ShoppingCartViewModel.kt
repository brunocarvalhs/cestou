package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.usecases.cart.ObserveShoppingCartUseCase
import br.com.brunocarvalhs.domain.usecases.product.CheckProductUseCase
import br.com.brunocarvalhs.domain.usecases.product.RemoveProductUseCase
import br.com.brunocarvalhs.domain.usecases.product.UpdateProductQuantityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingCartViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeShoppingCartUseCase: ObserveShoppingCartUseCase,
    private val updateProductQuantityUseCase: UpdateProductQuantityUseCase,
    private val removeProductUseCase: RemoveProductUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingCartUiState())
    val uiState: StateFlow<ShoppingCartUiState> = _uiState.asStateFlow()

    init {
        initializeCart()
    }

    fun onIntent(intent: ShoppingCartUiIntent) {
        when (intent) {
            is ShoppingCartUiIntent.RemoveItem -> removeProduct(productId = intent.productId)
            ShoppingCartUiIntent.Retry -> initializeCart()
            is ShoppingCartUiIntent.UpdateQuantity -> updateProductQuantity(
                productId = intent.productId,
                newQuantity = intent.quantity
            )
        }
    }

    private fun initializeCart() = viewModelScope.launch {
        _uiState.value = ShoppingCartUiState(isLoading = true)
        observeCart()
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    private fun observeCart() = viewModelScope.launch {
        observeShoppingCartUseCase.invoke().getOrNull()?.collect { cart ->
            updateUiState(
                cartId = cart.id,
                products = cart.products,
                totalPrice = cart.recalculateTotal(),
                token = cart.token,
            )
        }
    }

    private fun removeProduct(productId: String) = viewModelScope.launch {
        removeProductUseCase(productId)
            .onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Falha ao remover o produto",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateProductQuantity(productId: String, newQuantity: Int) = viewModelScope.launch {
        if (newQuantity < 1) return@launch

        updateProductQuantityUseCase(productId, newQuantity)
            .onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Falha ao atualizar a quantidade do produto",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUiState(
        cartId: String,
        products: List<Product>? = null,
        totalPrice: Long? = null,
        token: String? = null,
        isLoading: Boolean = false
    ) {
        val updatedProducts = products ?: _uiState.value.products
        val updatedTotalPrice = totalPrice ?: run {
            updatedProducts.sumOf { (it.price ?: 0) * it.quantity }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = isLoading,
            cartId = cartId,
            products = updatedProducts,
            totalPrice = updatedTotalPrice,
            token = token ?: _uiState.value.token,
        )
    }
}
