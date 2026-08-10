package br.com.brunocarvalhs.howmuch.feature.shopping.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.Settings
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.EditShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.JoinListContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.scanner.QrCodeBottomSheet
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.scanner.QrCodeScanner
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen.ShoppingScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.EditShoppingViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.JoinListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.ScannerViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.ShoppingListViewModel

private const val SCANNER_MAX_HEIGHT_FRACTION = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.shoppingGraph(
    navigator: Navigator,
    windowSizeClass: WindowSizeClass
) {
    composable<ShoppingList> {
        val viewModel: ShoppingListViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ShoppingScreen(
            uiState = uiState,
            windowSizeClass = windowSizeClass,
            intent = viewModel.intent,
            onSettings = { navigator.navigate(route = Settings) },
        )
    }

    shoppingEditDialog(navigator)
    shoppingOtherDialogs(navigator)
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.shoppingEditDialog(navigator: Navigator) {
    dialog<EditShopping>(
        typeMap = EditShopping.typeMap
    ) {
        val viewModel: EditShoppingViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            uiState.shopping?.let { shopping ->
                EditShoppingContent(
                    shopping = shopping,
                    onSave = { updated: Shopping ->
                        viewModel.intent.onUpdate(updated)
                    },
                    onCancel = { viewModel.intent.onCancel() },
                    onShareToken = {
                        viewModel.intent.onShareToken(shopping.id)
                    },
                    sharingToken = uiState.sharingToken
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.shoppingOtherDialogs(navigator: Navigator) {
    dialog<QrCode> { backStackEntry ->
        val route: QrCode = backStackEntry.toRoute()
        QrCodeBottomSheet(
            token = route.token,
            onDismissRequest = { navigator.goBack() },
            onShare = { /* Implement share if needed */ }
        )
    }

    dialog<Scanner> {
        val viewModel: ScannerViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            containerColor = Color.Black,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White) }
        ) {
            Box(modifier = Modifier.fillMaxHeight(SCANNER_MAX_HEIGHT_FRACTION)) {
                QrCodeScanner(
                    onTokenScanned = { token ->
                        viewModel.onTokenScanned(token)
                    },
                    onDismiss = { navigator.goBack() }
                )
            }
        }
    }

    dialog<JoinList> {
        val viewModel: JoinListViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = { viewModel.intent.onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            JoinListContent(
                onDismiss = { viewModel.intent.onDismiss() },
                onJoin = { token -> viewModel.intent.onJoinByToken(token) },
                onScan = { viewModel.intent.onScanQrCode() },
                error = uiState.error,
                isLoading = uiState.isLoading
            )
        }
    }
}
