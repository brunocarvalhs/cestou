package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthNavigationTest {

    @Test
    fun `Welcome is public`() {
        assertEquals(RouteType.PUBLIC, Welcome.routeType)
    }
}
