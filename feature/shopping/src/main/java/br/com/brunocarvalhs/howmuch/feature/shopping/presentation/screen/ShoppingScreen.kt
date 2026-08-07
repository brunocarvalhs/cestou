package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragTarget
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DropTarget
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.ai.CartAssistantDock
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.common.ShoppingEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.CreateShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingHeader
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItemLoading
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingSummaryCard
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState

private const val SCROLL_THRESHOLD_UP = -15f
private const val SCROLL_THRESHOLD_DOWN = 15f
private const val HOVER_ALPHA = 0.5f

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingScreen(
    uiState: ShoppingListUiState,
    windowSizeClass: WindowSizeClass,
    intent: ShoppingListIntent,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it.asString(context)
            )
        }
    }

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
                if (available.y < SCROLL_THRESHOLD_UP) isUiVisible = false
                if (available.y > SCROLL_THRESHOLD_DOWN) isUiVisible = true
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        intent.onFetchAll()
    }

    val totalSpent = remember(uiState.list) {
        uiState.list.sumOf { it.price }
    }
    val completedLists = remember(uiState.list) {
        uiState.list.count { it.status == Shopping.Status.FINISH }
    }
    val totalBudget = remember(uiState.list) {
        uiState.list.sumOf { it.budget ?: 0.0 }
    }

    DragAndDropContainer {
        Scaffold(
            topBar = {
                ShoppingHeader(
                    onAdd = { intent.onCreate() },
                    onJoin = { intent.onShowJoinDialog() },
                    onSettingsClick = onSettings
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (isExpanded) 400.dp else 300.dp),
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    focusManager.clearFocus()
                                }
                            },
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        shoppingSearchAndFilters(uiState, intent)

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ShoppingSummaryCard(
                                totalAmount = totalSpent,
                                totalLists = uiState.list.size,
                                completedLists = completedLists,
                                totalBudget = totalBudget
                            )
                        }

                        shoppingListContent(uiState, intent)
                    }

                    if (isExpanded) {
                        ShoppingAssistantExpanded(uiState, intent, padding)
                    }
                }

                if (!isExpanded) {
                    ShoppingAssistantCompact(
                        uiState = uiState,
                        intent = intent,
                        isVisible = isUiVisible || uiState.aiDockState != AiDockState.COLLAPSED || isKeyboardVisible
                    )
                }
            }
        }

        if (uiState.isCreateSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { intent.onShowCreateSheet(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                CreateShoppingContent(
                    onConfirm = { title, description -> intent.onCreateConfirmed(title, description) },
                    onCancel = { intent.onShowCreateSheet(false) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyGridScope.shoppingSearchAndFilters(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = uiState.searchQuery,
                        onQueryChange = { intent.onQueryChange(it) },
                        onSearch = { intent.onSearch(it) },
                        expanded = false,
                        onExpandedChange = {},
                        enabled = true,
                        placeholder = { Text(stringResource(R.string.shopping_management_search_placeholder)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = null,
                        colors = SearchBarDefaults.colors().inputFieldColors,
                        interactionSource = null,
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = SearchBarDefaults.inputFieldShape,
                colors = SearchBarDefaults.colors(),
                tonalElevation = SearchBarDefaults.TonalElevation,
                shadowElevation = SearchBarDefaults.ShadowElevation,
                windowInsets = SearchBarDefaults.windowInsets,
            ) { }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filters) { filter ->
                    FilterChip(
                        selected = filter == uiState.selectedFilter,
                        onClick = { intent.onFilter(filter) },
                        label = {
                            val label = when (filter) {
                                "Todos" -> stringResource(R.string.shopping_filter_all)
                                "Compras" -> stringResource(R.string.shopping_filter_active)
                                "Favoritos" -> stringResource(R.string.shopping_filter_favorites)
                                else -> filter
                            }
                            Text(label)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun LazyGridScope.shoppingListContent(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
    if (uiState.isLoading) {
        items(5) {
            ShoppingItemLoading(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    } else if (uiState.filteredList.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ShoppingEmptyState()
        }
    } else {
        uiState.groupedList.forEach { (month, shoppings) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 8.dp
                    )
                )
            }
            items(shoppings, key = { it.id }) { shopping ->
                val globalIndex = uiState.filteredList.indexOf(shopping)
                DropTarget(
                    onDataDropped = { data ->
                        if (data is Shopping) {
                            val fromIndex = uiState.filteredList.indexOf(data)
                            if (fromIndex != -1 && globalIndex != -1) {
                                intent.onMove(fromIndex, globalIndex)
                            }
                        }
                    }
                ) { isHovered, _ ->
                    val dragAlpha by animateFloatAsState(
                        targetValue = if (isHovered) HOVER_ALPHA else 1.0f,
                        label = "dragAlpha"
                    )

                    DragTarget(
                        modifier = Modifier.alpha(dragAlpha),
                        dataToDrop = shopping
                    ) {
                        ShoppingItem(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            onClick = { intent.onOpen(shopping.id) },
                            title = shopping.title,
                            description = shopping.description,
                            price = shopping.price,
                            budget = shopping.budget,
                            status = shopping.status,
                            isFavorite = shopping.isFavorite,
                            onFavoriteClick = { intent.onToggleFavorite(shopping) },
                            onDeleteClick = { intent.onDelete(shopping.id) },
                            onDuplicateClick = { intent.onDuplicate(shopping) },
                            onShareClick = { intent.onShare(shopping) },
                            onEditClick = { intent.onEdit(shopping) },
                            onFinishClick = {
                                if (shopping.status == Shopping.Status.FINISH) {
                                    intent.onReopen(shopping)
                                } else {
                                    intent.onUpdate(
                                        shopping.copy(status = Shopping.Status.FINISH)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingAssistantExpanded(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .width(400.dp)
            .padding(padding)
            .imePadding()
    ) {
        CartAssistantDock(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            state = AiDockState.CHAT, // Sempre Chat se expandido
            messages = uiState.aiMessages,
            loading = uiState.isAiLoading,
            value = uiState.prompt,
            onValueChange = { intent.onPromptChanged(it) },
            onSendClick = { intent.onSendPrompt() },
            onFocused = { },
            onNotFocused = { },
            onAddClick = { intent.onCreate() },
            onToggleClick = { },
            suggestions = uiState.aiSuggestions,
            isSuggestionsLoading = uiState.isAiSuggestionsLoading,
            onSuggestionClick = { intent.onSuggestionClick(it) }
        )
    }
}

@Composable
private fun BoxScope.ShoppingAssistantCompact(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent,
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
            messages = uiState.aiMessages,
            loading = uiState.isAiLoading,
            value = uiState.prompt,
            onValueChange = { intent.onPromptChanged(it) },
            onSendClick = { intent.onSendPrompt() },
            onFocused = { intent.onOpenAi() },
            onNotFocused = {
                intent.onFetchAll()
                intent.onCloseAi()
            },
            onAddClick = { intent.onCreate() },
            onToggleClick = { intent.onToggleAi() },
            suggestions = uiState.aiSuggestions,
            isSuggestionsLoading = uiState.isAiSuggestionsLoading,
            onSuggestionClick = { intent.onSuggestionClick(it) }
        )
    }
}

@Preview
@Composable
private fun ShoppingPreview() {
    // ShoppingScreen(
    //    uiState = ShoppingListUiState(), intent = ShoppingListIntent())
}
