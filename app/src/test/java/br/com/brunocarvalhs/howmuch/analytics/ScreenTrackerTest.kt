package br.com.brunocarvalhs.howmuch.analytics

import androidx.navigation.NavDestination
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.reflect.KClass

class ScreenTrackerTest {

    private class CartRoute
    private class OtherRoute

    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val graph = object : FeatureNavGraph {
        override val screenNames: Map<KClass<*>, ScreenName> =
            mapOf(CartRoute::class to ScreenName("cart", "CartScreen"))
    }
    private val current = mutableMapOf<NavDestination, KClass<*>>()
    private val tracker = ScreenTracker(analytics, setOf(graph)) { destination, route -> current[destination] == route }

    private fun destination(route: KClass<*>): NavDestination =
        mockk<NavDestination>(relaxed = true).also { current[it] = route }

    @Test
    fun `reports the screen name and the composable class never a view model`() {
        tracker.onDestinationChanged("e1", destination(CartRoute::class))

        verify { analytics.trackScreenView("cart", "CartScreen") }
    }

    @Test
    fun `does not report the same back stack entry twice`() {
        val dest = destination(CartRoute::class)

        tracker.onDestinationChanged("e1", dest)
        tracker.onDestinationChanged("e1", dest)

        verify(exactly = 1) { analytics.trackScreenView(any(), any()) }
    }

    @Test
    fun `reports again when the user comes back through a new entry`() {
        val dest = destination(CartRoute::class)

        tracker.onDestinationChanged("e1", dest)
        tracker.onDestinationChanged("e2", dest)

        verify(exactly = 2) { analytics.trackScreenView("cart", "CartScreen") }
    }

    @Test
    fun `ignores a destination that no feature mapped`() {
        tracker.onDestinationChanged("e1", destination(OtherRoute::class))

        verify(exactly = 0) { analytics.trackScreenView(any(), any()) }
    }

    @Test
    fun `reports back_pressed when the user returns to an earlier entry`() {
        val cart = destination(CartRoute::class)
        val other = destination(OtherRoute::class)
        val graphWithBoth = object : FeatureNavGraph {
            override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
                CartRoute::class to ScreenName("cart", "CartScreen"),
                OtherRoute::class to ScreenName("other", "OtherScreen")
            )
        }
        val both = ScreenTracker(analytics, setOf(graphWithBoth)) { d, r -> current[d] == r }

        both.onDestinationChanged("e1", cart)
        both.onDestinationChanged("e2", other)
        both.onDestinationChanged("e1", cart)

        verify {
            analytics.trackEvent(
                AnalyticsEvents.BACK_PRESSED,
                mapOf(AnalyticsParams.FROM to "other", AnalyticsParams.TO to "cart")
            )
        }
    }

    @Test
    fun `forward navigation never reports back_pressed`() {
        tracker.onDestinationChanged("e1", destination(CartRoute::class))
        tracker.onDestinationChanged("e2", destination(CartRoute::class))

        verify(exactly = 0) { analytics.trackEvent(AnalyticsEvents.BACK_PRESSED, any()) }
    }

    @Test
    fun `a tab switch reports tab_selected and is not a back`() {
        val cart = destination(CartRoute::class)
        val other = destination(OtherRoute::class)
        val graphWithBoth = object : FeatureNavGraph {
            override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
                CartRoute::class to ScreenName("cart", "CartScreen"),
                OtherRoute::class to ScreenName("other", "OtherScreen")
            )
        }
        val both = ScreenTracker(analytics, setOf(graphWithBoth)) { d, r -> current[d] == r }

        both.onDestinationChanged("e1", cart)
        both.onDestinationChanged("e2", other)
        both.onTabSelected(CartRoute())
        both.onDestinationChanged("e1", cart)

        verify { analytics.trackEvent(AnalyticsEvents.TAB_SELECTED, mapOf(AnalyticsParams.TO to "cart")) }
        verify(exactly = 0) { analytics.trackEvent(AnalyticsEvents.BACK_PRESSED, any()) }
    }

    @Test
    fun `reselecting the current tab does not swallow the next real back`() {
        val cart = destination(CartRoute::class)
        val other = destination(OtherRoute::class)
        val graphWithBoth = object : FeatureNavGraph {
            override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
                CartRoute::class to ScreenName("cart", "CartScreen"),
                OtherRoute::class to ScreenName("other", "OtherScreen")
            )
        }
        val both = ScreenTracker(analytics, setOf(graphWithBoth)) { d, r -> current[d] == r }

        both.onDestinationChanged("e1", cart)
        both.onDestinationChanged("e2", other)
        both.onTabSelected(OtherRoute())
        both.onDestinationChanged("e1", cart)

        verify {
            analytics.trackEvent(
                AnalyticsEvents.BACK_PRESSED,
                mapOf(AnalyticsParams.FROM to "other", AnalyticsParams.TO to "cart")
            )
        }
    }
}
