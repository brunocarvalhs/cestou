package br.com.brunocarvalhs.howmuch.app.foundation.navigation.routes

import kotlinx.serialization.Serializable

@Serializable
data class ProductGraphRoute(
    val cartId: String,
    val isProductListed: Boolean = false,
)