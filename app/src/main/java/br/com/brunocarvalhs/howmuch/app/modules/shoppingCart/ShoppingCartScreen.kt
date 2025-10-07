package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import android.widget.Toast
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.brunocarvalhs.data.model.ProductModel
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
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.PriceFieldBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShareCartBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShoppingCartCardsPager
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShoppingCartItem
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.TokenBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.TypeShopping
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.generateShareableCart
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.generateShareableToken
import kotlin.random.Random

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
    val listState = rememberLazyListState()

    var showTokenSheet by remember { mutableStateOf(false) }
    var showPriceBottomSheet by remember { mutableStateOf(false) }

    var selectedDestination by rememberSaveable { mutableIntStateOf(uiState.type.ordinal) }

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
            }
            item {
                PrimaryTabRow(selectedTabIndex = selectedDestination) {
                    TypeShopping.entries.forEachIndexed { index, destination ->
                        Tab(
                            selected = selectedDestination == index,
                            onClick = {
                                selectedDestination = index
                            },
                            text = {
                                Text(
                                    text = stringResource(destination.label),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
            if (uiState.isLoading) {
                items(count = numberCardLoading) {
                    ShoppingCartItem(isLoading = true)
                }
            } else {
                when (selectedDestination) {
                    TypeShopping.LIST.ordinal -> {
                        items(uiState.products.filter { !it.isChecked }) { item ->
                            ShoppingCartItem(
                                product = item,
                                onRemove = {
                                    onIntent(ShoppingCartUiIntent.RemoveItem(item.id))
                                    trackClick(
                                        viewId = "product_remove_${item.id}",
                                        viewName = "Remove Product",
                                        screenName = "ShoppingCartScreen"
                                    )
                                },
                                onQuantityChange = {
                                    onIntent(ShoppingCartUiIntent.UpdateQuantity(item.id, it))
                                    trackClick(
                                        viewId = "product_quantity_${item.id}",
                                        viewName = "Change Quantity",
                                        screenName = "ShoppingCartScreen"
                                    )
                                },
                                onCheckedChange = {
                                    showPriceBottomSheet = it
                                    trackClick(
                                        viewId = "product_checked_${item.id}",
                                        viewName = "Change Checked",
                                        screenName = "ShoppingCartScreen"
                                    )
                                }
                            )
                            if (showPriceBottomSheet) {
                                PriceFieldBottomSheet(
                                    onDismissRequest = { showPriceBottomSheet = false },
                                    onConfirmation = { price ->
                                        showPriceBottomSheet = false
                                        onIntent(
                                            ShoppingCartUiIntent.UpdateChecked(
                                                product = item,
                                                price = price,
                                                isChecked = !showPriceBottomSheet
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        items(uiState.products.filter { it.isChecked }) { product ->
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

    if (showProductSheet && uiState.cartId != null) {
        ProductFormBottomSheet(
            shoppingCartId = uiState.cartId,
            isProductListed = selectedDestination == TypeShopping.LIST.ordinal,
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

private class ShoppingCartStateProvider : PreviewParameterProvider<ShoppingCartUiState> {
    override val values: Sequence<ShoppingCartUiState>
        get() = sequenceOf(
            ShoppingCartUiState(
                isLoading = true,
            ),
            ShoppingCartUiState(
                isLoading = false,
                products = getProducts(),
                totalPrice = getTotalPrice(),
                token = null,
                type = TypeShopping.CART,
            ),
            ShoppingCartUiState(
                isLoading = false,
                products = getProducts(),
                totalPrice = getTotalPrice(),
                token = null,
                type = TypeShopping.LIST,
            ),
        )

    private fun getTotalPrice(): Long = getProducts().sumOf {
        if (it.isChecked) {
            (it.price ?: ZERO_NUMBER) * it.quantity
        } else {
            ZERO_NUMBER
        }
    }

    private fun getProducts(): List<ProductModel> {
        val isChecked = Random.nextBoolean()
        return LIST_NUMBER.map {
            ProductModel(
                name = "Product $it",
                price = if (isChecked) Random.nextLong(PRICE_NUMBER) else null,
                quantity = Random.nextInt(SIX_NUMBER),
                isChecked = isChecked
            )
        }
    }

    companion object {
        const val SIX_NUMBER = 6
        const val PRICE_NUMBER = 2000L

        const val ZERO_NUMBER = 0L

        val LIST_NUMBER = (1..100)
    }
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartContentPreview(
    @PreviewParameter(ShoppingCartStateProvider::class) uiState: ShoppingCartUiState
) {
    ShoppingCartContent(
        uiState = uiState,
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
