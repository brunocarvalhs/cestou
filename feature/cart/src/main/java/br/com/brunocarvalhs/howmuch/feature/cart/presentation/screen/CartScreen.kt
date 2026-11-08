package br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCategoryHeader
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouEmptyState
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouLockedBanner
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.exposeTestTags
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.CartBottomBar
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.CartBudgetLines
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.CartDetailHeader
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ProductListItem
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.budgetAlertText
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent.CartIntent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState
import kotlin.collections.component1
import kotlin.collections.component2

/** Chave da seção dos itens já pegos; o caractere nulo evita colidir com uma categoria real. */
internal const val IN_CART_SECTION_KEY = "\u0000in_cart"

/**
 * Agrupa por categoria; com o carrinho em tempo real, os itens marcados saem das categorias e vão
 * para uma seção própria no fim da lista.
 */
internal fun groupCartProducts(
    products: List<Product>,
    isCategorized: Boolean,
    separateInCart: Boolean
): Map<String, List<Product>> {
    val (inCart, pending) = if (separateInCart) {
        products.partition { it.isPurchased }
    } else {
        emptyList<Product>() to products
    }
    val grouped = if (isCategorized) pending.groupBy { it.category } else mapOf("" to pending)
    return if (inCart.isEmpty()) grouped else grouped + (IN_CART_SECTION_KEY to inCart)
}

private data class CartSummary(
    val currencyFormatter: CurrencyFormatter,
    val isCategorized: Boolean,
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
    onBack: () -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    onEditShopping: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    var isUiVisible by remember { mutableStateOf(false) }
    var collapsedCategories by remember { mutableStateOf(emptySet<String>()) }

    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    val currencyFormatter: CurrencyFormatter = rememberCurrencyFormatter()

    val isCategorized = uiState.shopping?.isCategorized ?: true
    val realtimeCart = uiState.realtimeCartEnabled
    val cartSummary = remember(uiState.products, currencyFormatter, isCategorized, realtimeCart) {
        CartSummary(
            currencyFormatter = currencyFormatter,
            isCategorized = isCategorized,
            groupedProducts = groupCartProducts(uiState.products.asReversed(), isCategorized, realtimeCart)
        )
    }
    val inCartTitle = stringResource(R.string.cart_section_in_cart)

    val listUpdatedMessage = stringResource(R.string.shopping_list_updated)
    val budgetAlertMessage = uiState.budgetAlert?.let { budgetAlertText(it) }

    // O aviso aparece uma vez; ao mostrar, avisa o ViewModel, que reporta o evento e limpa o estado.
    LaunchedEffect(uiState.budgetAlert) {
        if (budgetAlertMessage != null) {
            snackbarHostState.showSnackbar(message = budgetAlertMessage, duration = SnackbarDuration.Long)
            intent.onBudgetAlertShown()
        }
    }

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
                    description = uiState.shopping?.description.orEmpty(),
                    emoji = uiState.shopping?.emoji,
                    onBack = onBack,
                    footer = {
                        CartBudgetLines(
                            listProgress = uiState.listBudgetProgress,
                            monthProgress = uiState.monthProgress
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                intent.onShareShopping()
                            },
                            modifier = Modifier.testTag("cart_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(
                                    br.com.brunocarvalhs.howmuch.core.ui.R.string.action_share
                                )
                            )
                        }
                        IconButton(
                            onClick = {
                                isUiVisible = true
                            },
                            modifier = Modifier.testTag("cart_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(
                                    br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_more_options
                                )
                            )
                        }
                        DropdownMenu(
                            expanded = isUiVisible,
                            onDismissRequest = { isUiVisible = false },
                            modifier = Modifier.exposeTestTags()
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.shopping_list_edit_list_title))
                                },
                                modifier = Modifier.testTag("cart_menu_edit"),
                                onClick = {
                                    isUiVisible = false
                                    onEditShopping()
                                }
                            )
                            if (!isLocked) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.shopping_list_menu_clear_purchased)) },
                                    modifier = Modifier.testTag("cart_menu_clear_purchased"),
                                    onClick = { intent.onClearPurchased() }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.shopping_list_menu_clear_purchased)) },
                                    modifier = Modifier.testTag("cart_menu_clear_purchased"),
                                    onClick = { intent.onClearPurchased() }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    AnimatedVisibility(visible = !isLocked && !isExpanded) {
                        CartBottomBar(
                            currencyFormatter = currencyFormatter,
                            totalAmount = uiState.totals.planned,
                            cartAmount = uiState.totals.inCart.takeIf { uiState.realtimeCartEnabled },
                            hasUnpricedItems = uiState.totals.unpricedCount > 0,
                            onFinished = { intent.onToggleFinishPurchaseSheet() },
                            onAdd = { intent.onToggleProductPicker() }
                        )
                    }
                }
            },
            snackbarHost = {
                // The AI-chat FAB lives outside Scaffold's floatingActionButton slot (it's
                // manually placed in the content Box below), so Scaffold can't auto-avoid it —
                // push the snackbar up above the FAB's footprint instead of narrowing it.
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 88.dp)
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            // CartBottomBar (when visible) already applies its own navigationBarsPadding; only
            // add it here when that bar is absent (locked/expanded layout), otherwise the list
            // and FAB would get double bottom clearance.
            val contentModifier = if (isLocked || isExpanded) {
                Modifier
                    .padding(paddingValues)
                    .navigationBarsPadding()
            } else {
                Modifier.padding(paddingValues)
            }
            Box(modifier = contentModifier) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (isLocked) {
                        item {
                            CestouLockedBanner()
                        }
                    }

                    if (uiState.products.isEmpty()) {
                        item {
                            CestouEmptyState()
                        }
                    }

                    cartSummary.groupedProducts.forEach { (category, products) ->
                        val isCartSection = category == IN_CART_SECTION_KEY
                        val showHeader = cartSummary.isCategorized || isCartSection
                        val isCollapsed = showHeader && category in collapsedCategories

                        if (showHeader) {
                            item(key = "header_$category") {
                                CestouCategoryHeader(
                                    category = category,
                                    title = inCartTitle.takeIf { isCartSection },
                                    expanded = !isCollapsed,
                                    onToggle = {
                                        collapsedCategories = if (category in collapsedCategories) {
                                            collapsedCategories - category
                                        } else {
                                            collapsedCategories + category
                                        }
                                    }
                                )
                            }
                        }

                        if (isCollapsed) return@forEach

                        itemsIndexed(products, key = { _, product -> product.id }) { index, product ->
                            // Attribution avatar only makes sense once there's more than one member
                            // to attribute to (spec IAA-01 AC6), and only once the product actually
                            // has history to show — a legacy product with none renders like a
                            // single-member list instead of a clickable "no history yet" avatar
                            // (design.md Error Handling Strategy).
                            val showAttribution = (uiState.shopping?.users?.size ?: 0) > 1 &&
                                product.lastActivity != null
                            ProductListItem(
                                product = product,
                                enabled = !isLocked,
                                onDelete = { intent.onDeleteProduct(product) },
                                onEdit = { intent.onEditProduct(product) },
                                onTogglePurchased = { intent.onTogglePurchased(product, it) },
                                showDivider = index < uiState.products.size - 1,
                                showAttribution = showAttribution,
                                attributionProfile = if (showAttribution) {
                                    uiState.memberProfiles[product.lastActivity?.userId]
                                } else {
                                    null
                                },
                                onShowHistory = { intent.onShowProductHistory(product) }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onOpenAiChat,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("cart_ai_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(
                            br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_ai_icon
                        )
                    )
                }
            }
        }
    }
}

private val previewShopping = Shopping(
    id = "1",
    title = "Compras do mês",
    description = "Supermercado",
    price = 0.0,
    status = Shopping.Status.IN_PROGRESS,
    users = listOf("user-1"),
    roles = emptyMap()
)

private val previewProducts = listOf(
    Product(id = "1", name = "Arroz", quantity = 2.0, price = 25.0, category = "Mercearia"),
    Product(
        id = "2",
        name = "Feijão",
        quantity = 1.0,
        price = 8.5,
        category = "Mercearia",
        isPurchased = true
    ),
    Product(id = "3", name = "Leite", quantity = 6.0, price = 4.5, category = "Laticínios"),
    Product(id = "4", name = "Sabão em pó", quantity = 1.0, price = 18.9, category = "Limpeza")
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@PreviewCestouScreens
@Composable
private fun CartEmptyPreview() {
    CestouTheme {
        CartScreen(
            uiState = CartUiState(shopping = previewShopping),
            windowSizeClass = rememberPreviewWindowSizeClass()
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@PreviewCestouScreens
@Composable
private fun CartWithProductsPreview() {
    CestouTheme {
        CartScreen(
            uiState = CartUiState(
                shopping = previewShopping,
                products = StableList(previewProducts)
            ),
            windowSizeClass = rememberPreviewWindowSizeClass()
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@PreviewCestouScreens
@Composable
private fun CartLockedPreview() {
    CestouTheme {
        CartScreen(
            uiState = CartUiState(
                shopping = previewShopping.copy(status = Shopping.Status.FINISH),
                products = StableList(previewProducts)
            ),
            windowSizeClass = rememberPreviewWindowSizeClass()
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun rememberPreviewWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return WindowSizeClass.calculateFromSize(
        DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    )
}
