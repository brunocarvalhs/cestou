package br.com.brunocarvalhs.howmuch.feature.shopping

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.shoppingGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.wear.shoppingWearGraph
import javax.inject.Inject

class ShoppingNavGraph @Inject constructor() : FeatureNavGraph {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.shoppingGraph(navigator, windowSizeClass)
    }

    override fun registerWearGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator
    ) {
        navGraphBuilder.shoppingWearGraph(navigator)
    }
}
