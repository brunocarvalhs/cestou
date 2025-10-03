package br.com.brunocarvalhs.howmuch

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.NavBarItem
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.ProductGraphRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.ShoppingCartGraphRoute
import br.com.brunocarvalhs.howmuch.app.modules.base.BaseScreen
import br.com.brunocarvalhs.howmuch.app.modules.history.HistoryScreen
import br.com.brunocarvalhs.howmuch.app.modules.menu.MenuScreen
import br.com.brunocarvalhs.howmuch.app.modules.products.ProductFormScreen
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartScreen

@Composable
fun MainApp(
    navController: NavHostController,
) {
    NavHost(navController = navController, startDestination = ShoppingCartGraphRoute) {
        composable<ProductGraphRoute> { backStackEntry ->
            val route: ProductGraphRoute = backStackEntry.toRoute()
            ProductFormScreen(
                shoppingCartId = route.cartId,
                navController = navController,
                viewModel = hiltViewModel()
            )
        }
        composable<ShoppingCartGraphRoute> {
            BaseScreen(
                tabs = linkedMapOf(
                    NavBarItem.HOME to {
                        ShoppingCartScreen(
                            navController = navController,
                            viewModel = hiltViewModel()
                        )
                    },
                    NavBarItem.HISTORY to {
                        HistoryScreen(
                            navController = navController,
                            viewModel = hiltViewModel()
                        )
                    },
                    NavBarItem.MENU to {
                        MenuScreen()
                    }
                )
            )
        }
    }
}
