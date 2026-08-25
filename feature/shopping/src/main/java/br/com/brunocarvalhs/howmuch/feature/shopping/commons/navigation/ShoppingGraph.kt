package br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.core.navigation.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.Notifications
import br.com.brunocarvalhs.howmuch.core.navigation.QrCode
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.feature.settings.commons.navigation.Settings
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.form.EditShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.form.JoinListContent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.scanner.QrCodeBottomSheet
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.scanner.QrCodeScanner
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.screen.NotificationsScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.screen.ShoppingScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.EditShoppingViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.JoinListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.NotificationsViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.ScannerViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.ShoppingListViewModel

private const val SCANNER_MAX_HEIGHT_FRACTION = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.shoppingGraph(
    navigator: Navigator, windowSizeClass: WindowSizeClass
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

    composable<Notifications> {
        val viewModel: NotificationsViewModel = hiltViewModel()
        viewModel.onBack = { navigator.goBack() }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NotificationsScreen(
            state = uiState, intent = viewModel.intent
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
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.intent.onErrorShown()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                uiState.shopping?.let { shopping ->
                    EditShoppingContent(
                        shopping = shopping, onSave = { updated: Shopping ->
                        viewModel.intent.onUpdate(updated)
                    }, onCancel = { viewModel.intent.onCancel() }, onShareToken = {
                        viewModel.intent.onShareToken(shopping.id)
                    }, sharingToken = uiState.sharingToken
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.shoppingOtherDialogs(navigator: Navigator) {
    dialog<QrCode> { backStackEntry ->
        val route: QrCode = backStackEntry.toRoute()
        val context = LocalContext.current
        val shareText = stringResource(R.string.shopping_management_invite_share_text, route.token)

        QrCodeBottomSheet(
            token = route.token,
            onDismissRequest = { navigator.goBack() },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            })
    }

    dialog<Scanner> {
        val viewModel: ScannerViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            containerColor = Color.Black,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White) }) {
            Box(modifier = Modifier.fillMaxHeight(SCANNER_MAX_HEIGHT_FRACTION)) {
                QrCodeScanner(onTokenScanned = { token ->
                    viewModel.onTokenScanned(token)
                }, onDismiss = { navigator.goBack() })
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
                initialToken = uiState.initialToken ?: "",
                error = uiState.error,
                isLoading = uiState.isLoading
            )
        }
    }
}
