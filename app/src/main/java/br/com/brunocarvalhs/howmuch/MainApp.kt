//noinspection UsingMaterialAndMaterial3Libraries
package br.com.brunocarvalhs.howmuch

import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.EditProductRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.FinalizePurchaseRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.NavBarItem
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.ProductGraphRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.SharedCartBottomSheetRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.ShoppingCartGraphRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.SubscriptionGraphRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.TokenBottomSheetRoute
import br.com.brunocarvalhs.howmuch.app.modules.base.BaseScreen
import br.com.brunocarvalhs.howmuch.app.modules.editProduct.EditProductScreen
import br.com.brunocarvalhs.howmuch.app.modules.editProduct.EditProductViewModel
import br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase.FinalizePurchaseScreen
import br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase.FinalizePurchaseViewModel
import br.com.brunocarvalhs.howmuch.app.modules.history.HistoryScreen
import br.com.brunocarvalhs.howmuch.app.modules.menu.MenuScreen
import br.com.brunocarvalhs.howmuch.app.modules.products.ProductFormScreen
import br.com.brunocarvalhs.howmuch.app.modules.shared.SharedCartScreen
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartScreen
import br.com.brunocarvalhs.howmuch.app.modules.subscription.SubscriptionRoute
import br.com.brunocarvalhs.howmuch.app.modules.token.TokenScreen

@Composable
fun MainApp(
    navController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    isPremium: Boolean = false
) {
    ModalBottomSheetLayout(bottomSheetNavigator) {
        NavHost(navController = navController, startDestination = ShoppingCartGraphRoute) {
            composable<ShoppingCartGraphRoute> {
                BaseScreen(
                    tabs = linkedMapOf(
                        NavBarItem.HOME to {
                            ShoppingCartScreen(
                                isPremium = isPremium,
                                navController = navController,
                                viewModel = hiltViewModel()
                            )
                        },
                        NavBarItem.HISTORY to {
                            HistoryScreen(
                                viewModel = hiltViewModel()
                            )
                        },
                        NavBarItem.MENU to {
                            MenuScreen(
                                navController = navController
                            )
                        }
                    )
                )
            }
            composable<SubscriptionGraphRoute> {
                SubscriptionRoute(navController = navController)
            }
            bottomSheet<ProductGraphRoute> { backStackEntry ->
                val route: ProductGraphRoute = backStackEntry.toRoute()
                ProductFormScreen(
                    shoppingCartId = route.cartId,
                    isProductListed = route.isProductListed,
                    navController = navController,
                    viewModel = hiltViewModel()
                )
            }
            bottomSheet<TokenBottomSheetRoute> {
                TokenScreen(
                    navController = navController,
                    viewModel = hiltViewModel(),
                    isPremium = isPremium,
                )
            }
            bottomSheet<SharedCartBottomSheetRoute> {
                SharedCartScreen(
                    navController = navController,
                    viewModel = hiltViewModel(),
                    isPremium = isPremium,
                )
            }
            bottomSheet<FinalizePurchaseRoute> {
                FinalizePurchaseScreen(
                    navController = navController,
                    viewModel = hiltViewModel<FinalizePurchaseViewModel>()
                )
            }
            bottomSheet<EditProductRoute> {
                EditProductScreen(
                    navController = navController,
                    viewModel = hiltViewModel<EditProductViewModel>()
                )
            }
        }
    }
}
