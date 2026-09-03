package br.com.brunocarvalhs.howmuch.feature.cart.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ConfirmItemContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.EditItemContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.FinishPurchaseContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen.CartScreen
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.CartViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.ConfirmItemViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.EditItemViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.FinishPurchaseViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.ShareOptionsViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.ShareOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
internal fun NavGraphBuilder.cartGraph(
    navigator: Navigator,
    windowSizeClass: WindowSizeClass
) {
    navigation<CartFlow>(
        startDestination = CartDestination::class,
        typeMap = CartFlow.typeMap
    ) {
        cartDestination(navigator, windowSizeClass)
        finishPurchaseDestination(navigator)
        confirmItemDestination(navigator)
        editItemDestination(navigator)
        shareOptionsDestination(navigator)
    }
}

private fun NavGraphBuilder.cartDestination(
    navigator: Navigator,
    windowSizeClass: WindowSizeClass
) {
    composable<CartDestination> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<CartFlow>()
        }
        val viewModel: CartViewModel = hiltViewModel(parentEntry)
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        CartScreen(
            uiState = uiState,
            windowSizeClass = windowSizeClass,
            intent = viewModel.intent,
            onBack = { navigator.goBack() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.finishPurchaseDestination(navigator: Navigator) {
    dialog<FinishPurchaseRoute> { backStackEntry ->
        val viewModel: FinishPurchaseViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<CartFlow>()
        }
        val listViewModel: CartViewModel = hiltViewModel(parentEntry)
        val listUiState by listViewModel.uiState.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            listUiState.shopping?.let { shopping ->
                FinishPurchaseContent(
                    totalEstimate = listUiState.products.sumOf { it.total },
                    onFinish = { price, establishment ->
                        viewModel.onFinishPurchase(shopping, price, establishment)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.confirmItemDestination(navigator: Navigator) {
    dialog<ConfirmItemRoute>(
        typeMap = ConfirmItemRoute.typeMap
    ) { backStackEntry ->
        val route: ConfirmItemRoute = backStackEntry.toRoute()
        val viewModel: ConfirmItemViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ConfirmItemContent(
                product = route.product,
                onConfirm = { price, quantity ->
                    viewModel.onConfirmPurchased(route.product, price, quantity)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.editItemDestination(navigator: Navigator) {
    dialog<EditItemRoute>(
        typeMap = EditItemRoute.typeMap
    ) { backStackEntry ->
        val route: EditItemRoute = backStackEntry.toRoute()
        val viewModel: EditItemViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            EditItemContent(
                product = route.product,
                onSave = { name, category, price, quantity ->
                    viewModel.onSaveEdit(
                        product = route.product.copy(
                            name = name,
                            category = category
                        ),
                        price = price,
                        quantity = quantity,
                    )
                }
            )
        }
    }
}

private fun NavGraphBuilder.shareOptionsDestination(navigator: Navigator) {
    dialog<ShareOptionsRoute> { backStackEntry ->
        val viewModel: ShareOptionsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<CartFlow>()
        }
        val listViewModel: CartViewModel = hiltViewModel(parentEntry)
        val listUiState by listViewModel.uiState.collectAsStateWithLifecycle()

        ShareOptionsBottomSheet(
            onDismissRequest = { navigator.goBack() },
            onInviteMember = { listUiState.shopping?.let { viewModel.onInviteMember(it) } },
            onShareAsText = { listUiState.shopping?.let { viewModel.onShareAsText(it) } }
        )
    }
}
