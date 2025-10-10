package br.com.brunocarvalhs.howmuch.app.foundation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ShoppingCartGraphRoute

@Serializable
data class ProductGraphRoute(val cartId: String?)

@Serializable
data object HomeGraphRoute
