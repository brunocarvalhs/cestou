package br.com.brunocarvalhs.howmuch.app.foundation.navigation.routes

import kotlinx.serialization.Serializable

@Serializable
data class FinalizePurchaseRoute(
    val cartId: String,
    val price: Long,
)
