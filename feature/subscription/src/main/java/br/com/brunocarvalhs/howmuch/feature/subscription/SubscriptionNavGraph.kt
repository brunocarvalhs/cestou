package br.com.brunocarvalhs.howmuch.feature.subscription

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.subscription.navigation.subscriptionGraph
import javax.inject.Inject

internal class SubscriptionNavGraph @Inject constructor() : FeatureNavGraph {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.subscriptionGraph(navigator)
    }
}
