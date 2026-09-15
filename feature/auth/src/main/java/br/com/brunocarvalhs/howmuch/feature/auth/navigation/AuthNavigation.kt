package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import br.com.brunocarvalhs.howmuch.core.navigation.RouteProtocol
import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import kotlinx.serialization.Serializable

@Serializable
object Welcome : RouteProtocol {
    override val routeType: RouteType = RouteType.PUBLIC
}
