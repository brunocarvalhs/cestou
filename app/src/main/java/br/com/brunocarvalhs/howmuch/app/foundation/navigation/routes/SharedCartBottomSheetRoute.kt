package br.com.brunocarvalhs.howmuch.app.foundation.navigation.routes

import kotlinx.serialization.Serializable

@Serializable
data class SharedCartBottomSheetRoute(
    val cartId: String,
)
