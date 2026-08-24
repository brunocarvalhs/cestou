package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class NavigatorTest {

    private val navController = mockk<NavController>(relaxed = true)
    private val navigator = Navigator(navController)

    @Test
    fun `navigate forwards the route and options builder to NavController`() {
        // Tipado como Any (não String) para casar com o overload genérico
        // NavController.navigate(T, NavOptionsBuilder.() -> Unit) que Navigator.kt de fato invoca.
        val route: Any = "shopping_list"
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
