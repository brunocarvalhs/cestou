package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.brunocarvalhs.data.model.ProductModel
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvents.trackEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsParam
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.setStatusBarIconColor
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.FinalizePurchaseRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.ProductGraphRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.SharedCartBottomSheetRoute
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.TokenBottomSheetRoute
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.HeaderComponent
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.PriceFieldBottomSheet
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShoppingCartCardsPager
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.ShoppingCartItem
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers.TypeShopping
import kotlin.random.Random

@Composable
fun ShoppingCartScreen(
    navController: NavController,
    viewModel: ShoppingCartViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        (context as Activity).window.setStatusBarIconColor(false)
        trackEvent(
            event = AnalyticsEvent.SCREEN_VIEW,
            params = mapOf(
                AnalyticsParam.SCREEN_NAME to "ShoppingCartScreen",
            )
        )
    }

    ShoppingCartContent(
        navController = navController,
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onAddToCart = { cartId, isProductListed ->
            navController.navigate(
                route = ProductGraphRoute(
                    cartId = cartId,
                    isProductListed = isProductListed
                )
            )
        },
        onCheckout = {
            navController.navigate(
                route = FinalizePurchaseRoute(
                    cartId = uiState.cartId,
                    price = uiState.totalPrice
                )
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartContent(
    navController: NavController,
    uiState: ShoppingCartUiState,
    onIntent: (ShoppingCartUiIntent) -> Unit,
    onAddToCart: (String?, Boolean) -> Unit = { _, _ -> },
    onCheckout: () -> Unit = {},
    numberCardLoading: Int = 3
) {
    val listState = rememberLazyListState()

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
                    navController.navigate(TokenBottomSheetRoute(uiState.token))
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
                    onAddToCart(uiState.cartId, selectedDestination == TypeShopping.LIST.ordinal)
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
                        navController.navigate(
                            SharedCartBottomSheetRoute(
                                cartId = uiState.cartId,
                                token = uiState.token,
                            )
                        )
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
                                Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = stringResource(destination.label)
                                    )
                                    Text(
                                        text = stringResource(destination.label),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                products = getProducts().map { it.toCopy(isChecked = false) },
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
@DevicesPreview
private fun ShoppingCartContentPreview(
    @PreviewParameter(ShoppingCartStateProvider::class) uiState: ShoppingCartUiState
) {
    ShoppingCartContent(
        navController = NavController(LocalContext.current),
        uiState = uiState,
        onIntent = {},
        onAddToCart = { _, _ -> },
        onCheckout = {},
    )
}
