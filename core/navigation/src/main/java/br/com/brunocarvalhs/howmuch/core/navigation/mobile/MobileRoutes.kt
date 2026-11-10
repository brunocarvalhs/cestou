package br.com.brunocarvalhs.howmuch.core.navigation.mobile

import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.RouteProtocol
import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import br.com.brunocarvalhs.howmuch.core.navigation.navTypeSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class AiChat(val shoppingId: String? = null, val sessionId: String? = null) : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

@Serializable
data object Profile : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

/** Bottom-bar tab listing finished purchases grouped by month (sdd-029). */
@Serializable
data object History : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

/** Monthly spending chart and category ranking, opened from [History] (sdd-031). */
@Serializable
data object SpendingInsights : NavKey, RouteProtocol {
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

/**
 * Watch only: the screen that starts a pairing session (sdd-055). Replaces the old 6-digit
 * `PairingCode` route.
 */
@Serializable
data object PairingWear : NavKey

/**
 * Phone only: the pairing screen opened by the watch's deeplink `cestou://wear/pair` (sdd-055).
 * Public because `:app` navigates to it and `:feature:auth` registers it.
 *
 * @param sessionId base64url id chosen by the watch; every message of the session carries it.
 * @param wearPublicKey the watch's ephemeral EC P-256 public key, base64url (X.509).
 * @param wearNodeId the watch's Wearable node id, where the phone sends its answer.
 */
@Serializable
data class WearPairing(
    val sessionId: String,
    val wearPublicKey: String,
    val wearNodeId: String
) : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

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

/**
 * The entry point into `feature:subscription`'s subscription-management screen (AD-010) — where
 * an already-PRO user sees their plan and is sent to Google Play to cancel or change it. [Paywall]
 * stays the only entry point for FREE users; this route is unreachable without an active
 * subscription (Play Billing is the single source of truth for that, never this app).
 */
@Serializable
data object ManageSubscription : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

/**
 * Abre o In-App Review da Play (sdd-034). Rota sem conteúdo visível, separada de [FeedbackSurvey]
 * para o `screen_view` das pesquisas não contar pedidos de avaliação.
 */
@Serializable
data object StoreReview : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

/**
 * Pesquisa de satisfação (sdd-034), aberta pelo `:app` sobre um destino estável quando o
 * `FeedbackPromptRequester` tem um pedido pendente. [kind] é o nome de `SurveyKind` e [feature] o
 * recurso medido no CSAT; ficam como `String` para a rota continuar serializável sem depender do
 * enum.
 */
@Serializable
data class FeedbackSurvey(val kind: String, val feature: String? = null) : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}
