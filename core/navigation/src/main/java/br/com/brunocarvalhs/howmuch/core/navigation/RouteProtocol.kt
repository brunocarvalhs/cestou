package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * Whether a route requires an authenticated user to be reachable.
 */
enum class RouteType {
    PUBLIC,
    PROTECTED
}

/**
 * Contract a route object implements to declare its own [RouteType], so navigation guards can
 * ask "does this route require auth" as a property of the route itself instead of matching
 * against a hardcoded list that has to be kept in sync by hand every time a route is added.
 */
interface RouteProtocol {
    val routeType: RouteType
}

/**
 * Checks this destination's hierarchy (so a nested screen inside a protected graph still
 * matches) against [routes], returning true if any matching route is [RouteType.PROTECTED].
 */
fun NavDestination.isProtectedRoute(routes: List<RouteProtocol>): Boolean =
    routes.any { route ->
        route.routeType == RouteType.PROTECTED && hierarchy.any { it.hasRoute(route::class) }
    }
