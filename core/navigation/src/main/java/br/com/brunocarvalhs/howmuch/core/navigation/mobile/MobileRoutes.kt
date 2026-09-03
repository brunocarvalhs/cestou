package br.com.brunocarvalhs.howmuch.core.navigation.mobile

import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navTypeSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data object AiChat : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data class JoinList(val token: String? = null) : NavKey

@Serializable
data object Notifications : NavKey

@Serializable
data object AiSettings : NavKey

@Serializable
data class QrCode(val token: String) : NavKey

@Serializable
data object PairingCode : NavKey

@Serializable
data object LinkPhone : NavKey

@Serializable
data object LinkWearDevice : NavKey

@Serializable
data class CartFlow(val shopping: Shopping) : NavKey {
    companion object {
        val typeMap = mapOf(
            typeOf<Shopping>() to navTypeSerializer<Shopping>()
        )
    }
}
