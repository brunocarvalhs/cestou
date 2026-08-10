package br.com.brunocarvalhs.howmuch.feature.products.navigation

import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navTypeSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class ProductsFlow(val shopping: Shopping) : NavKey {
    companion object {
        val typeMap = mapOf(
            typeOf<Shopping>() to navTypeSerializer<Shopping>()
        )
    }
}
