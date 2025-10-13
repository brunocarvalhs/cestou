package br.com.brunocarvalhs.howmuch.app.foundation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ShoppingCartGraphRoute

@Serializable
data class ProductGraphRoute(
    val cartId: String,
    val isProductListed: Boolean = false,
)

@Serializable
data class EditProductRoute(
    val cartId: String,
    val productId: String,
    val isEditName: Boolean = false,
    val isEditPrice: Boolean = false,
    val isEditQuantity: Boolean = false,
    val name: String? = null,
    val price: Long? = null,
    val quantity: Int = 0,
    val isChecked: Boolean = false,
)

@Serializable
data class TokenBottomSheetRoute(val token: String)

@Serializable
data class SharedCartBottomSheetRoute(
    val cartId: String,
)

@Serializable
data class FinalizePurchaseRoute(
    val cartId: String,
    val price: Long,
)