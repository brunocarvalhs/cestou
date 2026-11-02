package br.com.brunocarvalhs.howmuch.feature.cart.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Paywall
import br.com.brunocarvalhs.howmuch.core.ui.components.ShareOptionsBottomSheet
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ConfirmItemContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.EditItemContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.FinishPurchaseContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ProductHistoryContent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen.CartScreen
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.CartSubscriptionGateViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.CartViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.ConfirmItemViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.EditItemViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.FinishPurchaseViewModel
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel.ShareOptionsViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.EditShopping

private const val PAYWALL_SOURCE_SHARE = "cart_share"

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
        productHistoryDestination(navigator)
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

        /** Avoids NavBackStackEntry.getViewModelStore() throwing after `parentEntry` is destroyed (Crashlytics fatal). */
        val parentEntryLifecycleState by parentEntry.lifecycle.currentStateAsState()
        if (parentEntryLifecycleState == Lifecycle.State.DESTROYED) return@composable
        val viewModel: CartViewModel = hiltViewModel(parentEntry)
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val gateViewModel: CartSubscriptionGateViewModel = hiltViewModel()
        val isPro by gateViewModel.isPro.collectAsStateWithLifecycle()

        CartScreen(
            uiState = uiState,
            windowSizeClass = windowSizeClass,
            intent = viewModel.intent.copy(
                onShareShopping = {
                    if (isPro) {
                        viewModel.intent.onShareShopping()
                    } else {
                        navigator.navigate(Paywall(source = PAYWALL_SOURCE_SHARE))
                    }
                }
            ),
            onBack = { navigator.goBack() },
            onOpenAiChat = {
                uiState.shopping?.id?.let { shoppingId ->
                    navigator.navigate(AiChat(shoppingId = shoppingId))
                }
            },
            onEditShopping = {
                uiState.shopping?.let { shopping ->
                    navigator.navigate(EditShopping(shopping))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.finishPurchaseDestination(navigator: Navigator) {
    dialog<FinishPurchaseRoute> { backStackEntry ->
        val viewModel: FinishPurchaseViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<CartFlow>()
        }

        /** Avoids NavBackStackEntry.getViewModelStore() throwing after `parentEntry` is destroyed (Crashlytics fatal). */
        val parentEntryLifecycleState by parentEntry.lifecycle.currentStateAsState()
        if (parentEntryLifecycleState == Lifecycle.State.DESTROYED) return@dialog
        val listViewModel: CartViewModel = hiltViewModel(parentEntry)
        val listUiState by listViewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it.asString(context))
                viewModel.clearError()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                listUiState.shopping?.let { shopping ->
                    FinishPurchaseContent(
                        totalEstimate = listUiState.products.sumOf { it.total },
                        onFinish = { price, establishment ->
                            viewModel.onFinishPurchase(shopping, price, establishment)
                        }
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
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

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.productHistoryDestination(navigator: Navigator) {
    dialog<ProductHistoryRoute>(
        typeMap = ProductHistoryRoute.typeMap
    ) { backStackEntry ->
        val route: ProductHistoryRoute = backStackEntry.toRoute()
        val parentEntry = remember(backStackEntry) {
            navigator.navController.getBackStackEntry<CartFlow>()
        }

        /** Avoids NavBackStackEntry.getViewModelStore() throwing after `parentEntry` is destroyed (Crashlytics fatal). */
        val parentEntryLifecycleState by parentEntry.lifecycle.currentStateAsState()
        if (parentEntryLifecycleState == Lifecycle.State.DESTROYED) return@dialog
        // Reuses the parent CartViewModel's already-resolved memberProfiles — no new
        // UserRepository call for this sheet (T10 done-when).
        val listViewModel: CartViewModel = hiltViewModel(parentEntry)
        val listUiState by listViewModel.uiState.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ProductHistoryContent(
                history = route.product.history,
                memberProfiles = listUiState.memberProfiles
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

        /** Avoids NavBackStackEntry.getViewModelStore() throwing after `parentEntry` is destroyed (Crashlytics fatal). */
        val parentEntryLifecycleState by parentEntry.lifecycle.currentStateAsState()
        if (parentEntryLifecycleState == Lifecycle.State.DESTROYED) return@dialog
        val listViewModel: CartViewModel = hiltViewModel(parentEntry)
        val listUiState by listViewModel.uiState.collectAsStateWithLifecycle()

        ShareOptionsBottomSheet(
            onDismissRequest = { navigator.goBack() },
            onInviteMember = { listUiState.shopping?.let { viewModel.onInviteMember(it) } },
            onShareAsText = { listUiState.shopping?.let { viewModel.onShareAsText(it) } }
        )
    }
}
