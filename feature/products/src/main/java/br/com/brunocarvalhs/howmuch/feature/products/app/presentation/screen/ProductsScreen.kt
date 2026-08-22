package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.LocalDragTargetInfo
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouDarkGreen
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouSoftGreen
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTextPrimary
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTextSecondary
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.ai.CartAssistantDock
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart.CartDetailHeader
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart.MoveToBar
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.CategoryHeader
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.EmptyState
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.LockedBanner
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.SummaryCard
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product.ProductListItem
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductsListIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductsListUiState

private const val SCROLL_THRESHOLD = 15f
private val GRID_CELLS_ADAPTIVE_EXPANDED = 400.dp
private val GRID_CELLS_ADAPTIVE_COMPACT = 300.dp
private val ASSISTANT_EXPANDED_WIDTH = 400.dp

private data class ProductsSummary(
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
internal fun ProductsScreen(
    uiState: ProductsListUiState,
    windowSizeClass: WindowSizeClass,
    intent: ProductsListIntent = ProductsListIntent(),
    onBack: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    val listState = rememberLazyGridState()
    var isUiVisible by remember { mutableStateOf(true) }

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (uiState.aiDockState != AiDockState.COLLAPSED) {
                    isUiVisible = true
                    return Offset.Zero
                }
                if (available.y < -SCROLL_THRESHOLD) isUiVisible = false
                if (available.y > SCROLL_THRESHOLD) isUiVisible = true
                return Offset.Zero
            }
        }
    }

    val currencyFormatter = rememberCurrencyFormatter()

    val productsSummary = remember(uiState.products, currencyFormatter) {
        ProductsSummary(
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
                if (isUiVisible) {
                    ProductsTopBar(
                        title = uiState.shopping?.title ?: stringResource(R.string.shopping_list_default_title),
                        usersCount = uiState.shopping?.users?.size ?: 0,
                        showFinish = uiState.products.isNotEmpty() && !isLocked,
                        isLocked = isLocked,
                        onBack = onBack,
                        onFinish = { intent.onToggleFinishPurchaseSheet() },
                        onShare = { intent.onShowShareOptions() },
                        onClearPurchased = { intent.onClearPurchased() }
                    )
                }
            },
            bottomBar = {
                if (!isLocked && !isExpanded) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total estimado:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CestouTextSecondary
                            )
                            Text(
                                text = currencyFormatter.format(productsSummary.totalAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                color = CestouTextPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { intent.onToggleProductPicker() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CestouBrightGreen)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar Item", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(
                            if (isExpanded) GRID_CELLS_ADAPTIVE_EXPANDED else GRID_CELLS_ADAPTIVE_COMPACT
                        ),
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .nestedScroll(nestedScrollConnection)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    focusManager.clearFocus()
                                }
                            },
                        contentPadding = PaddingValues(bottom = if (!isLocked) 160.dp else 16.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EditingStatusBar()
                        }

                        productsListContent(
                            uiState = uiState,
                            intent = intent,
                            isLocked = isLocked,
                            summary = productsSummary
                        )
                    }

                    if (isExpanded) {
                        ProductsAssistantExpanded(uiState, intent)
                    }
                }

                if (!isExpanded) {
                    val isAssistantVisible = (isUiVisible
                        || uiState.aiDockState != AiDockState.COLLAPSED
                        || isKeyboardVisible)
                        && !isLocked

                    ProductsAssistantCompact(
                        uiState = uiState,
                        intent = intent,
                        isVisible = isAssistantVisible
                    )
                }

                MoveToBar(
                    visible = LocalDragTargetInfo.current.isDragging,
                    shoppings = uiState.allShoppings,
                    onMove = { product, targetId ->
                        intent.onMoveProduct(product as Product, targetId)
                    }
                )
            }
        }
    }
}

@Composable
private fun EditingStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CestouBrightGreen.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(CestouBrightGreen)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "João está editando... Batata Inglesa",
            style = MaterialTheme.typography.labelSmall,
            color = CestouBrightGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProductsTopBar(
    title: String,
    usersCount: Int,
    showFinish: Boolean,
    isLocked: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onShare: () -> Unit,
    onClearPurchased: () -> Unit,
    modifier: Modifier = Modifier
) {
    CartDetailHeader(
        title = title,
        onBack = onBack,
        onShare = onShare,
        onEdit = { },
        usersCount = usersCount,
        showFinish = showFinish,
        onFinish = onFinish,
        actionsMore = {
            if (!isLocked) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.shopping_list_menu_clear_purchased)) },
                    onClick = onClearPurchased
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.productsListContent(
    uiState: ProductsListUiState,
    intent: ProductsListIntent,
    isLocked: Boolean,
    summary: ProductsSummary
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

@Composable
private fun ProductsAssistantExpanded(
    uiState: ProductsListUiState,
    intent: ProductsListIntent
) {
    Box(
        modifier = Modifier
            .width(ASSISTANT_EXPANDED_WIDTH)
            .padding(16.dp)
            .imePadding()
    ) {
        CartAssistantDock(
            modifier = Modifier.fillMaxSize(),
            state = AiDockState.CHAT,
            suggestions = uiState.aiSuggestions,
            isSuggestionsLoading = uiState.isAiSuggestionsLoading,
            messages = uiState.aiMessages,
            loading = uiState.isAiLoading,
            value = uiState.prompt,
            onValueChange = { intent.onPromptChanged(it) },
            onSendClick = { intent.onSendPrompt() },
            onFocused = { },
            onNotFocused = { },
            onAddClick = { intent.onToggleProductPicker() },
            onToggleClick = { },
            onSuggestionClick = { intent.onSuggestionClick(it) }
        )
    }
}

@Composable
private fun BoxScope.ProductsAssistantCompact(
    uiState: ProductsListUiState,
    intent: ProductsListIntent,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
    ) {
        CartAssistantDock(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            state = uiState.aiDockState,
            suggestions = uiState.aiSuggestions,
            isSuggestionsLoading = uiState.isAiSuggestionsLoading,
            messages = uiState.aiMessages,
            loading = uiState.isAiLoading,
            value = uiState.prompt,
            onValueChange = { intent.onPromptChanged(it) },
            onSendClick = { intent.onSendPrompt() },
            onFocused = { intent.onOpenAi() },
            onNotFocused = { intent.onCloseAi() },
            onAddClick = { intent.onToggleProductPicker() },
            onToggleClick = { intent.onToggleAi() },
            onSuggestionClick = { intent.onSuggestionClick(it) }
        )
    }
}

@Preview
@Composable
private fun ProductsPreview() {
    // ProductsScreen(
    //    uiState = ProductsListUiState(shopping = null)
    // )
}
