package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.data.model.ShoppingCartModel
import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.useCases.CreateShoppingCartUseCase
import br.com.brunocarvalhs.domain.useCases.EnterShoppingCartWithTokenUseCase
import br.com.brunocarvalhs.domain.useCases.FinalizePurchaseUseCase
import br.com.brunocarvalhs.domain.useCases.ObserveShoppingCartUseCase
import br.com.brunocarvalhs.domain.useCases.UpdateShoppingCartUseCase
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
class ShoppingCartViewModel @Inject constructor(
    private val createShoppingCartUseCase: CreateShoppingCartUseCase,
    private val observeShoppingCartUseCase: ObserveShoppingCartUseCase,
    private val enterShoppingCartWithTokenUseCase: EnterShoppingCartWithTokenUseCase,
    private val updateShoppingCartUseCase: UpdateShoppingCartUseCase,
    private val finalizePurchaseUseCase: FinalizePurchaseUseCase,
    private val cartLocalStorage: ICartLocalStorage
) : ViewModel() {

    private var cartId: String? = null
    private val _uiState = MutableStateFlow(ShoppingCartUiState())
    val uiState: StateFlow<ShoppingCartUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ShoppingCartUiEffect>()
    val uiEffect: SharedFlow<ShoppingCartUiEffect> = _uiEffect.asSharedFlow()

    init {
        initializeCart()
    }

    fun onIntent(intent: ShoppingCartUiIntent) {
        when (intent) {
            is ShoppingCartUiIntent.RemoveItem -> removeProduct(productId = intent.productId)
            ShoppingCartUiIntent.Retry -> initializeCart()
            is ShoppingCartUiIntent.SearchByToken -> searchByToken(token = intent.token)
            is ShoppingCartUiIntent.UpdateQuantity -> updateProductQuantity(
                productId = intent.productId,
                newQuantity = intent.quantity
            )

            is ShoppingCartUiIntent.FinalizePurchase -> finalizePurchase(
                market = intent.market,
                totalPrice = intent.totalPrice
            )

            is ShoppingCartUiIntent.UpdateChecked -> checkProduct(
                product = intent.product,
                price = intent.price,
                isChecked = intent.isChecked
            )
        }
    }

    private fun initializeCart() = viewModelScope.launch {
        _uiState.value = ShoppingCartUiState(isLoading = true)
        cartLocalStorage.getCartNow()?.let { cachedCart ->
            cartId = cachedCart.id
            observeCart(cartId = cachedCart.id)
        } ?: run {
            val newCart = ShoppingCartModel()
            cartId = newCart.id
            createAndObserveCart(newCart)
        }
    }

    private fun searchByToken(token: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val result = enterShoppingCartWithTokenUseCase(token).getOrNull()
        if (result != null) {
            cartId = result.id
            cartLocalStorage.saveCartNow(result)
            observeCart(result.id)
        } else {
            _uiEffect.emit(ShoppingCartUiEffect.ShowError("Invalid token"))
        }
    }

    private fun createAndObserveCart(shoppingCart: ShoppingCartModel) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        createShoppingCartUseCase(shoppingCart).onSuccess { cart ->
            cartId = cart.id
            observeCart(cart.id)
            cartLocalStorage.saveCartNow(cart)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }.onFailure {
            _uiEffect.emit(ShoppingCartUiEffect.ShowError("Failed to create shopping cart"))
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun observeCart(cartId: String) = viewModelScope.launch {
        observeShoppingCartUseCase(cartId).collect { cart ->
            updateUiState(
                products = cart.products,
                totalPrice = cart.recalculateTotal(),
                token = cart.token,
            )
        }
    }

    private fun removeProduct(productId: String) = viewModelScope.launch {
        val currentProducts = _uiState.value.products.toMutableList()
        val productToRemove = currentProducts.find { it.id == productId }

        if (productToRemove != null) {
            currentProducts.remove(productToRemove)

            cartLocalStorage.getCartNow()?.let { cart ->
                val updatedCart = cart.toCopy(products = currentProducts)
                try {
                    updateShoppingCartUseCase(updatedCart)
                    updateUiState(products = currentProducts)
                } catch (_: Exception) {
                    _uiEffect.emit(ShoppingCartUiEffect.ShowError("Failed to remove product"))
                }
            }
        } else {
            _uiEffect.emit(ShoppingCartUiEffect.ShowError("Product not found"))
        }
    }

    private fun updateProductQuantity(productId: String, newQuantity: Int) = viewModelScope.launch {
        if (newQuantity < 1) return@launch

        val currentProducts = _uiState.value.products.toMutableList()
        val productIndex = currentProducts.indexOfFirst { it.id == productId }

        if (productIndex != -1) {
            val oldProduct = currentProducts[productIndex]
            val updatedProduct = oldProduct.toCopy(quantity = newQuantity)
            currentProducts[productIndex] = updatedProduct

            cartLocalStorage.getCartNow()?.let { cart ->
                val updatedCart = cart.toCopy(products = currentProducts)
                try {
                    updateShoppingCartUseCase(updatedCart)
                    updateUiState(products = currentProducts)
                } catch (_: Exception) {
                    _uiEffect.emit(ShoppingCartUiEffect.ShowError("Failed to update product quantity"))
                }
            }
        } else {
            _uiEffect.emit(ShoppingCartUiEffect.ShowError("Product not found"))
        }
    }

    private fun updateUiState(
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

    private fun finalizePurchase(
        market: String,
        totalPrice: Long
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        cartId?.let { id ->
            finalizePurchaseUseCase(id, market = market, price = totalPrice).onSuccess {
                initializeCart()
                _uiEffect.emit(ShoppingCartUiEffect.ShowError("Compra finalizada com sucesso!"))
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure {
                Log.e(
                    this@ShoppingCartViewModel.javaClass.simpleName,
                    "Failed to finalize purchase",
                    it
                )
                _uiEffect.emit(ShoppingCartUiEffect.ShowError("Falha ao finalizar a compra"))
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        } ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false)
            _uiEffect.emit(ShoppingCartUiEffect.ShowError("Carrinho não encontrado"))
        }
    }

    fun checkProduct(
        product: Product,
        price: Long,
        isChecked: Boolean
    ) = viewModelScope.launch {
        val currentProducts = _uiState.value.products.toMutableList()
        val productIndex = currentProducts.indexOfFirst { it.id == product.id }

        if (productIndex != -1) {
            val oldProduct = currentProducts[productIndex]
            val updatedProduct = oldProduct.toCopy(
                price = price,
                isChecked = isChecked
            )
            currentProducts[productIndex] = updatedProduct

            cartLocalStorage.getCartNow()?.let { cart ->
                val updatedCart = cart.toCopy(products = currentProducts)
                try {
                    updateShoppingCartUseCase(updatedCart)
                    updateUiState(products = currentProducts)
                } catch (_: Exception) {
                    _uiEffect.emit(ShoppingCartUiEffect.ShowError("Failed to update product"))
                }
            }
        } else {
            _uiEffect.emit(ShoppingCartUiEffect.ShowError("Product not found"))
        }
    }
}
