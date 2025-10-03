package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import br.com.brunocarvalhs.domain.entities.Product


data class ShoppingCartUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val totalPrice: Long = 0L,
    val token: String? = null,
    val amountSaved: Long = 0L,
    val id: String? = null,
)
