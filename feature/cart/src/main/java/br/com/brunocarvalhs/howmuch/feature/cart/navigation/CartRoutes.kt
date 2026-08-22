package br.com.brunocarvalhs.howmuch.feature.cart.navigation

import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navTypeSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class CartFlow(val shopping: Shopping) : NavKey {
    companion object {
        val typeMap = mapOf(
            typeOf<Shopping>() to navTypeSerializer<Shopping>()
        )
    }
}

@Serializable
internal data object CartDestination : NavKey

@Serializable
internal data object FinishPurchaseRoute : NavKey

@Serializable
internal data class ConfirmItemRoute(val product: Product, val shoppingId: String) : NavKey {
    companion object {
        val typeMap = mapOf(typeOf<Product>() to navTypeSerializer<Product>())
    }
}

@Serializable
internal data class EditItemRoute(val product: Product, val shoppingId: String) : NavKey {
    companion object {
        val typeMap = mapOf(typeOf<Product>() to navTypeSerializer<Product>())
    }
}

@Serializable
internal data object ShareOptionsRoute : NavKey

@Serializable
internal data class QrCodeProductsRoute(val token: String) : NavKey
