package br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.CartBottomBar
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.CartDetailHeader
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ProductListItem
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent.CartIntent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.CategoryHeader
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.EmptyState
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.LockedBanner
import kotlin.collections.component1
import kotlin.collections.component2

private const val SCROLL_THRESHOLD = 15f
private val GRID_CELLS_ADAPTIVE_EXPANDED = 400.dp
private val GRID_CELLS_ADAPTIVE_COMPACT = 300.dp
private val ASSISTANT_EXPANDED_WIDTH = 400.dp

private data class CartSummary(
    val totalAmount: Double,
    val cartAmount: Double,
    val purchasedCount: Int,
    val currencyFormatter: CurrencyFormatter,
    val groupedProducts: Map<String, List<Product>>
)

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
internal fun CartScreen(
    uiState: CartUiState,
    windowSizeClass: WindowSizeClass? = null,
    intent: CartIntent = CartIntent(),
    onBack: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    var isUiVisible by remember { mutableStateOf(false) }

    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    val currencyFormatter: CurrencyFormatter = rememberCurrencyFormatter()

    val cartSummary = remember(uiState.products, currencyFormatter) {
        CartSummary(
            totalAmount = uiState.products.sumOf { it.total },
            cartAmount = uiState.products.sumOf { if (it.isPurchased) it.total else 0.0 },
            purchasedCount = uiState.products.count { it.isPurchased },
            currencyFormatter = currencyFormatter,
            groupedProducts = uiState.products.asReversed().groupBy { it.category }
        )
    }

    val listUpdatedMessage = stringResource(R.string.shopping_list_updated)

    LaunchedEffect(uiState.products.size) {
        if (uiState.products.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = listUpdatedMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                intent.onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && uiState.aiDockState != AiDockState.CHAT) {
            focusManager.clearFocus()
        }
    }

    val isLocked = uiState.shopping?.status == Shopping.Status.FINISH

    DragAndDropContainer {
        Scaffold(
            topBar = {
                CartDetailHeader(
                    title = uiState.shopping?.title
                        ?: stringResource(R.string.shopping_list_default_title),
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = {
                                isUiVisible = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = isUiVisible,
                            onDismissRequest = { isUiVisible = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.shopping_list_menu_clear_purchased)) },
                                onClick = { intent.onClearPurchased() }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    AnimatedVisibility(visible = !isLocked && !isExpanded) {
                        CartBottomBar(
                            currencyFormatter = currencyFormatter,
                            totalAmount = cartSummary.totalAmount,
                            onClick = { intent.onToggleProductPicker() }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            LazyColumn(
               modifier = Modifier.padding(paddingValues)
            ) {
                if (isLocked) {
                    item {
                        LockedBanner()
                    }
                }

                if (uiState.products.isEmpty()) {
                    item {
                        EmptyState()
                    }
                }

                cartSummary.groupedProducts.forEach { (category, products) ->
                    item {
                        CategoryHeader(category = category)
                    }

                    itemsIndexed(products) { index, product ->
                        ProductListItem(
                            product = product,
                            enabled = !isLocked,
                            onDelete = { intent.onDeleteProduct(product) },
                            onEdit = { intent.onEditProduct(product) },
                            onQuantityChange = { intent.onUpdateQuantity(product, it) },
                            onTogglePurchased = { intent.onTogglePurchased(product, it) },
                            showDivider = index < uiState.products.size - 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.cartListContent(
    uiState: CartUiState,
    intent: CartIntent,
    isLocked: Boolean,
    summary: CartSummary
) {
    if (isLocked) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LockedBanner()
        }
    }

    if (uiState.products.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState()
        }
    }

    summary.groupedProducts.forEach { (category, products) ->
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryHeader(category = category)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column {
                    products.forEachIndexed { index, product ->
                        ProductListItem(
                            product = product,
                            enabled = !isLocked,
                            onDelete = { intent.onDeleteProduct(product) },
                            onEdit = { intent.onEditProduct(product) },
                            onQuantityChange = { intent.onUpdateQuantity(product, it) },
                            onTogglePurchased = { intent.onTogglePurchased(product, it) },
                            showDivider = index < products.size - 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview
@Composable
private fun CartPreview() {
    CestouTheme {
        CartScreen(
            uiState = CartUiState(shopping = null),
        )
    }

}
