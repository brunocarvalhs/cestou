package br.com.brunocarvalhs.howmuch.feature.shopping

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.History
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.NewShopping
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Notifications
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.QrCode
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.SpendingInsights
import br.com.brunocarvalhs.howmuch.core.navigation.toScreenName
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.EditShopping
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.Scanner
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.shoppingGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.wear.shoppingWearGraph
import javax.inject.Inject
import kotlin.reflect.KClass

class ShoppingNavGraph @Inject constructor() : FeatureNavGraph {

    override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
        ShoppingList::class to AnalyticsScreen.SHOPPING_LIST.toScreenName(),
        NewShopping::class to AnalyticsScreen.SHOPPING_LIST.toScreenName(),
        Notifications::class to AnalyticsScreen.NOTIFICATIONS.toScreenName(),
        History::class to AnalyticsScreen.HISTORY.toScreenName(),
        SpendingInsights::class to AnalyticsScreen.SPENDING_INSIGHTS.toScreenName(),
        EditShopping::class to AnalyticsScreen.SHOPPING_EDIT.toScreenName(),
        QrCode::class to AnalyticsScreen.SHOPPING_QR_CODE.toScreenName(),
        Scanner::class to AnalyticsScreen.SHOPPING_JOIN_SCANNER.toScreenName(),
        JoinList::class to AnalyticsScreen.SHOPPING_JOIN.toScreenName()
    )

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
