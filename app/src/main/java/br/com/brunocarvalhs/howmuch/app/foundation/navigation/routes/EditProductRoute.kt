package br.com.brunocarvalhs.howmuch.app.foundation.navigation.routes

import kotlinx.serialization.Serializable

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
