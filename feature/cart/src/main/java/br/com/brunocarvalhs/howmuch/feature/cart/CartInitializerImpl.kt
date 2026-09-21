package br.com.brunocarvalhs.howmuch.feature.cart

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.cartGraph
import javax.inject.Inject

internal class CartInitializerImpl @Inject constructor() : CartInitializer {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.cartGraph(navigator, windowSizeClass)
    }

    override fun registerWearGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator
    ) {
    }
}
