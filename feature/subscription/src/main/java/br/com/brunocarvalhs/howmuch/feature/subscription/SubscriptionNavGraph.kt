package br.com.brunocarvalhs.howmuch.feature.subscription

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.ManageSubscription
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Paywall
import br.com.brunocarvalhs.howmuch.core.navigation.toScreenName
import br.com.brunocarvalhs.howmuch.feature.subscription.navigation.subscriptionGraph
import javax.inject.Inject
import kotlin.reflect.KClass

internal class SubscriptionNavGraph @Inject constructor() : FeatureNavGraph {

    override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
        Paywall::class to AnalyticsScreen.PAYWALL.toScreenName(),
        ManageSubscription::class to AnalyticsScreen.MANAGE_SUBSCRIPTION.toScreenName()
    )

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.subscriptionGraph(navigator)
    }
}
