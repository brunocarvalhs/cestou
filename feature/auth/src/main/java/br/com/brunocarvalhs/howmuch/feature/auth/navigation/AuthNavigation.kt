package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import br.com.brunocarvalhs.howmuch.core.navigation.RouteProtocol
import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import kotlinx.serialization.Serializable

@Serializable
object Welcome : RouteProtocol {
    override val routeType: RouteType = RouteType.PUBLIC
}

/** Required-name step (spec EPA-06); the app routes any signed-in account without a name here. */
@Serializable
object CompleteName : RouteProtocol {
    override val routeType: RouteType = RouteType.PUBLIC
}

@Serializable
internal data class EmailSignIn(val email: String = "") : RouteProtocol {
    override val routeType: RouteType get() = RouteType.PUBLIC
}

@Serializable
internal object EmailSignUp : RouteProtocol {
    override val routeType: RouteType = RouteType.PUBLIC
}

@Serializable
internal data class PasswordReset(val email: String = "") : RouteProtocol {
    override val routeType: RouteType get() = RouteType.PUBLIC
}

/**
 * True while the user is inside sign-in/sign-up or the name step. An account briefly has no name
 * between account creation and saving it, so the required-name gate must not fire here.
 */
fun NavDestination.isAuthFlow(): Boolean = hierarchy.any { destination ->
    destination.hasRoute(Welcome::class) ||
        destination.hasRoute(CompleteName::class) ||
        destination.hasRoute(EmailSignIn::class) ||
        destination.hasRoute(EmailSignUp::class) ||
        destination.hasRoute(PasswordReset::class)
}
