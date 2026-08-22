package br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation

import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navTypeSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
internal data class EditShopping(val shopping: Shopping) : NavKey {
    companion object {
        val typeMap = mapOf(typeOf<Shopping>() to navTypeSerializer<Shopping>())
    }
}

@Serializable
internal data class QrCode(val token: String) : NavKey

@Serializable
internal data object Scanner : NavKey
