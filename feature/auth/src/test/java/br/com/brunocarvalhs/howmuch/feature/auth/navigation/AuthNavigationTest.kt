package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthNavigationTest {

    @Test
    fun `Welcome is public`() {
        assertEquals(RouteType.PUBLIC, Welcome.routeType)
    }

    @Test
    fun `e-mail routes are public and carry the e-mail handed over between screens`() {
        assertEquals(RouteType.PUBLIC, EmailSignIn().routeType)
        assertEquals(RouteType.PUBLIC, EmailSignUp.routeType)
        assertEquals(RouteType.PUBLIC, PasswordReset().routeType)
        assertEquals("", EmailSignIn().email)
        assertEquals("ana@test.com", PasswordReset(email = "ana@test.com").email)
    }

    @Test
    fun `the required-name step is reachable before the app's protected routes`() {
        assertEquals(RouteType.PUBLIC, CompleteName.routeType)
    }
}
