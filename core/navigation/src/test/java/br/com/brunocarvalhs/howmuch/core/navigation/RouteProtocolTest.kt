package br.com.brunocarvalhs.howmuch.core.navigation

import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in each route's declared [RouteType]. A route silently flipping from PROTECTED to
 * PUBLIC here would reopen the sign-out guard bypass this contract exists to prevent.
 */
class RouteProtocolTest {

    @Test
    fun `ShoppingList is protected`() {
        assertEquals(RouteType.PROTECTED, ShoppingList.routeType)
    }

    @Test
    fun `AiChat is protected`() {
        assertEquals(RouteType.PROTECTED, AiChat.routeType)
    }

    @Test
    fun `Profile is protected`() {
        assertEquals(RouteType.PROTECTED, Profile.routeType)
    }
}
