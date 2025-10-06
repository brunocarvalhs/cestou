package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.TypeShopping

data class ShoppingCartUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val totalPrice: Long = 0L,
    val token: String? = null,
    val cartId: String? = null,
    val type: TypeShopping = TypeShopping.CART,
    val list: List<Product> = emptyList()
)
