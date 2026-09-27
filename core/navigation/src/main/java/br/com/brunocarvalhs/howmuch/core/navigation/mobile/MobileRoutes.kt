package br.com.brunocarvalhs.howmuch.core.navigation.mobile

import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.RouteProtocol
import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import br.com.brunocarvalhs.howmuch.core.navigation.navTypeSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class AiChat(val shoppingId: String) : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

@Serializable
data object Profile : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

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

/**
 * The single reachable entry point into `feature:subscription`'s paywall (AD-010). No
 * feature module may import that module directly - a paid feature that wants to show the paywall
 * navigates here instead, exactly like every other cross-feature boundary in `core/navigation`.
 *
 * @param source Where the paywall was opened from (e.g. "cart", "ai-agent"), for funnel
 * attribution - cheap to plumb through now, expensive to retrofit once the screen ships.
 */
@Serializable
data class Paywall(val source: String) : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}
