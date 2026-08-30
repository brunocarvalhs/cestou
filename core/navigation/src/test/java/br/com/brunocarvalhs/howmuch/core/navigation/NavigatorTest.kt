package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
}
