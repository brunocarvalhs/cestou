package br.com.brunocarvalhs.howmuch.feature.products.commons.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart.ConfirmItemContent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart.EditItemContent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart.FinishPurchaseContent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.ShareOptionsBottomSheet
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.screen.ProductScreen
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.screen.ProductsScreen
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.ConfirmItemViewModel
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.EditItemViewModel
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.FinishPurchaseViewModel
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.ProductsListViewModel
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.ShareOptionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.productsGraph(
    navigator: Navigator,
    windowSizeClass: WindowSizeClass
) {
    navigation<ProductsFlow>(
        startDestination = Products::class,
        typeMap = ProductsFlow.typeMap
    ) {
        productsDestination(navigator, windowSizeClass)
        productPickerDestination(navigator)
        finishPurchaseDestination(navigator)
        confirmItemDestination(navigator)
        editItemDestination(navigator)
        shareOptionsDestination(navigator)
        qrCodeDestination()
    }
}

private fun NavGraphBuilder.productsDestination(
    navigator: Navigator,
    windowSizeClass: WindowSizeClass
) {
    composable<Products> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<ProductsFlow>()
        }
        val viewModel: ProductsListViewModel = hiltViewModel(parentEntry)
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ProductsScreen(
            uiState = uiState,
            windowSizeClass = windowSizeClass,
            intent = viewModel.intent,
            onBack = { navigator.goBack() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.productPickerDestination(navigator: Navigator) {
    dialog<ProductPickerRoute>(
        typeMap = ProductPickerRoute.typeMap
    ) { backStackEntry ->
        val route: ProductPickerRoute = backStackEntry.toRoute()
        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ProductScreen(
                shopping = route.shopping,
                onBack = { navigator.goBack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.finishPurchaseDestination(navigator: Navigator) {
    dialog<FinishPurchaseRoute> { backStackEntry ->
        val viewModel: FinishPurchaseViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<ProductsFlow>()
        }
        val listViewModel: ProductsListViewModel = hiltViewModel(parentEntry)
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
                onSave = { name, category, price, quantity, unit ->
                    viewModel.onSaveEdit(route.product.copy(name = name, category = category), price, quantity, unit)
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
            navigator.navController.getBackStackEntry<ProductsFlow>()
        }
        val listViewModel: ProductsListViewModel = hiltViewModel(parentEntry)
        val listUiState by listViewModel.uiState.collectAsStateWithLifecycle()

        ShareOptionsBottomSheet(
            onDismissRequest = { navigator.goBack() },
            onInviteMember = { listUiState.shopping?.let { viewModel.onInviteMember(it) } },
            onShareAsText = { listUiState.shopping?.let { viewModel.onShareAsText(it) } }
        )
    }
}

private fun NavGraphBuilder.qrCodeDestination() {
    dialog<QrCodeProductsRoute> {
        // QrCodeBottomSheet(
        //     token = route.token,
        //     onDismissRequest = { navigator.goBack() },
        //     onShare = { listViewModel.intent.onShareShopping() }
        // )
        Text(stringResource(R.string.product_error_qr_not_available))
    }
}
