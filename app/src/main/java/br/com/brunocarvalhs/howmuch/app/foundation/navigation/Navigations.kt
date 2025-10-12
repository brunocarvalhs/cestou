package br.com.brunocarvalhs.howmuch.app.foundation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ShoppingCartGraphRoute

@Serializable
data class ProductGraphRoute(val cartId: String?)

@Serializable
data class TokenBottomSheetRoute(val token: String?)

@Serializable
data class SharedCartBottomSheetRoute(
    val cartId: String?,
    val token: String?,
)