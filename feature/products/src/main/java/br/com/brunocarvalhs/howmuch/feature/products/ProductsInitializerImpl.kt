package br.com.brunocarvalhs.howmuch.feature.products

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.commons.navigation.productsGraph
import javax.inject.Inject

internal class ProductsInitializerImpl @Inject constructor() : ProductsInitializer {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.productsGraph(navigator)
    }
}
