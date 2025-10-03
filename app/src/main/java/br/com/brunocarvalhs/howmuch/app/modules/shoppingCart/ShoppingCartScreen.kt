package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvents.trackEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsParam
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.shareText
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import br.com.brunocarvalhs.howmuch.app.modules.products.ProductFormBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.FinalizePurchaseDialog
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.HeaderComponent
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShareCartBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShoppingCartCardsPager
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShoppingCartItem
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.TokenBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.generateShareableCart
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.generateShareableToken

@Composable
fun ShoppingCartScreen(
    viewModel: ShoppingCartViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEffect by viewModel.uiEffect.collectAsState(initial = null)

    var showProductSheet by rememberSaveable { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        trackEvent(
            event = AnalyticsEvent.SCREEN_VIEW,
            params = mapOf(
                AnalyticsParam.SCREEN_NAME to "ShoppingCartScreen",
            )
        )
    }

    LaunchedEffect(uiEffect) {
        uiEffect?.let { effect ->
            when (effect) {
                is ShoppingCartUiEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is ShoppingCartUiEffect.NavigateToAddProduct -> {
                    showProductSheet = true
                }

                is ShoppingCartUiEffect.ShareCart -> {
                    showShareSheet = true
                }
            }
        }
    }

    ShoppingCartContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onAddToCart = { showProductSheet = true },
        onShared = { showShareSheet = true },
        onCheckout = { showFinalizeDialog = true },
        onShareList = {
            context.shareText(
                subject = context.getString(R.string.share_shopping_cart),
                text = generateShareableCart(uiState.products, uiState.totalPrice)
            )
            showShareSheet = false
        },
        onShareToken = {
            context.shareText(
                subject = context.getString(R.string.share_cart_token),
                text = generateShareableToken(uiState.token)
            )
            showShareSheet = false
        },
        showProductSheet = showProductSheet,
        showShareSheet = showShareSheet,
        showFinalizeDialog = showFinalizeDialog,
        setShowProductSheet = { showProductSheet = it },
        setShowShareSheet = { showShareSheet = it },
        setShowFinalizeDialog = { showFinalizeDialog = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartContent(
    uiState: ShoppingCartUiState,
    onIntent: (ShoppingCartUiIntent) -> Unit,
    onAddToCart: () -> Unit = {},
    onShared: () -> Unit = {},
    onCheckout: () -> Unit = {},
    onShareList: () -> Unit,
    onShareToken: () -> Unit,
    showProductSheet: Boolean,
    showShareSheet: Boolean,
    showFinalizeDialog: Boolean,
    setShowProductSheet: (Boolean) -> Unit,
    setShowShareSheet: (Boolean) -> Unit,
    setShowFinalizeDialog: (Boolean) -> Unit,
    numberCardLoading: Int = 3
) {
    var showTokenSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val showTitle by remember {
        derivedStateOf {
            val firstVisibleItem = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            firstVisibleItem > ZERO_INT || offset > 100
        }
    }

    Scaffold(
        topBar = {
            HeaderComponent(
                title = if (showTitle) {
                    stringResource(
                        R.string.currency,
                        uiState.totalPrice.toCurrencyString()
                    )
                } else {
                    null
                },
                onShared = {
                    showTokenSheet = true
                    trackClick(
                        viewId = "header_share_cart",
                        viewName = "Header Share Cart",
                        screenName = "ShoppingCartScreen"
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                onClick = {
                    onAddToCart()
                    trackClick(
                        viewId = "btn_add_product",
                        viewName = "Add Product FAB",
                        screenName = "ShoppingCartScreen"
                    )
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_product),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ShoppingCartCardsPager(
                    uiState = uiState,
                    onCheckout = {
                        onCheckout()
                        trackClick(
                            viewId = "pager_checkout",
                            viewName = "Checkout Pager",
                            screenName = "ShoppingCartScreen"
                        )
                    },
                    onShared = {
                        onShared()
                        trackClick(
                            viewId = "pager_share",
                            viewName = "Share Pager",
                            screenName = "ShoppingCartScreen"
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (uiState.isLoading) {
                items(count = numberCardLoading) {
                    ShoppingCartItem(isLoading = true)
                }
            } else {
                items(uiState.products) { product ->
                    ShoppingCartItem(
                        product = product,
                        onRemove = {
                            onIntent(ShoppingCartUiIntent.RemoveItem(product.id))
                            trackClick(
                                viewId = "product_remove_${product.id}",
                                viewName = "Remove Product",
                                screenName = "ShoppingCartScreen"
                            )
                        },
                        onQuantityChange = {
                            onIntent(ShoppingCartUiIntent.UpdateQuantity(product.id, it))
                            trackClick(
                                viewId = "product_quantity_${product.id}",
                                viewName = "Change Quantity",
                                screenName = "ShoppingCartScreen"
                            )
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    TokenBottomSheet(
        showSheet = showTokenSheet,
        onDismiss = {
            showTokenSheet = false
            trackClick(
                viewId = "token_sheet_dismiss",
                viewName = "Dismiss Token Sheet",
                screenName = "ShoppingCartScreen"
            )
        },
        onSubmit = { code ->
            onIntent(ShoppingCartUiIntent.SearchByToken(token = code))
            trackClick(
                viewId = "token_sheet_submit",
                viewName = "Submit Token",
                screenName = "ShoppingCartScreen"
            )
        }
    )

    if (showProductSheet && uiState.id != null) {
        ProductFormBottomSheet(
            shoppingCartId = uiState.id,
            onDismiss = {
                setShowProductSheet(false)
                trackClick(
                    viewId = "product_sheet_dismiss",
                    viewName = "Dismiss Product Sheet",
                    screenName = "ShoppingCartScreen"
                )
            }
        )
    }

    if (showShareSheet) {
        ShareCartBottomSheet(
            token = uiState.token,
            onShareList = {
                onShareList()
                trackClick(
                    viewId = "share_list",
                    viewName = "Share List",
                    screenName = "ShoppingCartScreen"
                )
            },
            onShareToken = {
                onShareToken()
                trackClick(
                    viewId = "share_token",
                    viewName = "Share Token",
                    screenName = "ShoppingCartScreen"
                )
            },
            onDismiss = {
                setShowShareSheet(false)
                trackClick(
                    viewId = "share_sheet_dismiss",
                    viewName = "Dismiss Share Sheet",
                    screenName = "ShoppingCartScreen"
                )
            }
        )
    }

    if (showFinalizeDialog) {
        FinalizePurchaseDialog(
            onDismiss = {
                setShowFinalizeDialog(false)
                trackClick(
                    viewId = "finalize_dialog_dismiss",
                    viewName = "Dismiss Finalize Dialog",
                    screenName = "ShoppingCartScreen"
                )
            },
            onSubmit = { name, price ->
                onIntent(
                    ShoppingCartUiIntent.FinalizePurchase(
                        market = name,
                        totalPrice = price
                    )
                )
                trackClick(
                    viewId = "finalize_dialog_submit",
                    viewName = "Submit Finalize Dialog",
                    screenName = "ShoppingCartScreen"
                )
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartContentPreview() {
    ShoppingCartContent(
        uiState = ShoppingCartUiState(),
        onIntent = {},
        onAddToCart = {},
        onShared = {},
        onCheckout = {},
        onShareList = {},
        onShareToken = {},
        showProductSheet = false,
        showShareSheet = false,
        showFinalizeDialog = false,
        setShowProductSheet = {},
        setShowShareSheet = {},
        setShowFinalizeDialog = {}
    )
}
