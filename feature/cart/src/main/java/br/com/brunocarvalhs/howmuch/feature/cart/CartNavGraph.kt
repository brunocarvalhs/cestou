package br.com.brunocarvalhs.howmuch.feature.cart

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import br.com.brunocarvalhs.howmuch.core.navigation.toScreenName
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.CartDestination
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ConfirmItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.EditItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.FinishPurchaseRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ProductHistoryRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ShareOptionsRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.cartGraph
import javax.inject.Inject
import kotlin.reflect.KClass

internal class CartNavGraph @Inject constructor() : FeatureNavGraph {

    override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
        CartDestination::class to AnalyticsScreen.CART.toScreenName(),
        FinishPurchaseRoute::class to AnalyticsScreen.CART_FINISH_PURCHASE.toScreenName(),
        ConfirmItemRoute::class to AnalyticsScreen.CART_CONFIRM_ITEM.toScreenName(),
        EditItemRoute::class to AnalyticsScreen.CART_EDIT_ITEM.toScreenName(),
        ProductHistoryRoute::class to AnalyticsScreen.CART_PRODUCT_HISTORY.toScreenName(),
        ShareOptionsRoute::class to AnalyticsScreen.CART_SHARE_OPTIONS.toScreenName()
    )

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.cartGraph(navigator, windowSizeClass)
    }
}
