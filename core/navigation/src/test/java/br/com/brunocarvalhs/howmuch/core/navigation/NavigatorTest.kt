package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class NavigatorTest {

    private val navController = mockk<NavController>(relaxed = true)
    private val navigator = Navigator().also { it.bind(navController) }

    @Test
    fun `navigate forwards the route and options builder to NavController`() {
        // Navigator.navigate() smart-casts a String route at runtime, which resolves to the
        // NavController.navigate(String, NavOptionsBuilder.() -> Unit) overload - not the
        // reified NavController.navigate(T, ...) one. `route` must be statically typed String
        // here too, otherwise verify() resolves the other overload and never matches the call.
        val route: String = "shopping_list"
        val builderSlot = slot<NavOptionsBuilder.() -> Unit>()

        navigator.navigate(route)

        verify { navController.navigate(route, capture(builderSlot)) }
    }

    @Test
    fun `goBack pops the back stack`() {
        navigator.goBack()

        verify { navController.popBackStack() }
    }

    @Test
    fun `navControllerOrNull returns the bound controller`() {
        assertSame(navController, navigator.navControllerOrNull)
    }

    @Test
    fun `navControllerOrNull is null before any bind`() {
        val freshNavigator = Navigator()

        assertNull(freshNavigator.navControllerOrNull)
    }

    @Test
    fun `unbind clears the currently bound controller`() {
        navigator.unbind(navController)

        assertNull(navigator.navControllerOrNull)
        assertThrows(IllegalStateException::class.java) { navigator.navController }
    }

    @Test
    fun `unbind does nothing when called with a stale controller that was already replaced`() {
        // Reproduces the crash scenario: an old Activity's composition is disposed (and calls
        // unbind with ITS NavController) after a new Activity has already bound a fresh one -
        // the stale unbind must not wipe out the newer, valid bind.
        val staleNavController = navController
        val freshNavController = mockk<NavController>(relaxed = true)
        navigator.bind(freshNavController)

        navigator.unbind(staleNavController)

        assertSame(freshNavController, navigator.navControllerOrNull)
    }

    @Test
    fun `unbind followed by bind of a different controller still works as a normal rebind`() {
        navigator.unbind(navController)
        val newNavController = mockk<NavController>(relaxed = true)
        navigator.bind(newNavController)

        assertSame(newNavController, navigator.navControllerOrNull)

        navigator.goBack()
        verify { newNavController.popBackStack() }
    }
}
