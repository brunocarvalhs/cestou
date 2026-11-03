package br.com.brunocarvalhs.howmuch.analytics

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@Serializable
internal data object ListsRoute

@Serializable
internal data object CartRoute

/**
 * Liga o [ScreenTracker] a um `NavHost` de verdade, do mesmo jeito que a `MainActivity`, para
 * provar o que os testes com `hasRoute` falso não provam: a rota real vira o `screen_name`
 * esperado, voltar emite `back_pressed` e nada usa o nome de uma ViewModel.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ScreenTrackingNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val graph = object : FeatureNavGraph {
        override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
            ListsRoute::class to ScreenName("shopping_list", "ShoppingScreen"),
            CartRoute::class to ScreenName("cart", "CartScreen")
        )
    }
    private val tracker = ScreenTracker(analytics, setOf(graph))
    private lateinit var navController: NavHostController

    private fun setContent() = compose.setContent {
        navController = rememberNavController()
        val entry by navController.currentBackStackEntryAsState()
        LaunchedEffect(entry) { entry?.let { tracker.onDestinationChanged(it.id, it.destination) } }
        NavHost(navController, startDestination = ListsRoute) {
            composable<ListsRoute> { Text("lists") }
            composable<CartRoute> { Text("cart") }
        }
    }

    @Test
    fun `lists then cart reports each screen by its composable class`() {
        setContent()

        compose.runOnUiThread { navController.navigate(CartRoute) }
        compose.waitForIdle()

        verifyOrder {
            analytics.trackScreenView("shopping_list", "ShoppingScreen")
            analytics.trackScreenView("cart", "CartScreen")
        }
    }

    @Test
    fun `going back reports the screen again and a back_pressed from cart to lists`() {
        setContent()
        compose.runOnUiThread { navController.navigate(CartRoute) }
        compose.waitForIdle()

        compose.runOnUiThread { navController.popBackStack() }
        compose.waitForIdle()

        verify(exactly = 2) { analytics.trackScreenView("shopping_list", "ShoppingScreen") }
        verify {
            analytics.trackEvent(
                AnalyticsEvents.BACK_PRESSED,
                mapOf(AnalyticsParams.FROM to "cart", AnalyticsParams.TO to "shopping_list")
            )
        }
    }
}
