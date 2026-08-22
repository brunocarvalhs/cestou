package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.screen

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragTarget
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DropTarget
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTextPrimary
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.ai.CartAssistantDock
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.common.ShoppingEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.form.CreateShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping.ShoppingHeader
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping.ShoppingItem
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping.ShoppingItemLoading
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping.ShoppingSummaryCard
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.ShoppingListUiState

private const val SCROLL_THRESHOLD_UP = -15f
private const val SCROLL_THRESHOLD_DOWN = 15f
private const val HOVER_ALPHA = 0.5f
private const val LOADING_ITEMS_COUNT = 5

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
            floatingActionButton = {
                if (isUiVisible && !isExpanded) {
                    ExtendedFloatingActionButton(
                        onClick = { intent.onCreate() },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Nova Lista") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    )
                }
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
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ShoppingSummaryCard(
                                totalAmount = totalSpent,
                                totalLists = uiState.list.size,
                                completedLists = completedLists,
                                totalBudget = totalBudget
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                ShoppingSearchBar(uiState, intent)
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Minhas Listas",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = CestouTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
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
@Composable
private fun ShoppingSearchBar(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = uiState.searchQuery,
                onQueryChange = { intent.onQueryChange(it) },
                onSearch = { intent.onSearch(it) },
                expanded = false,
                onExpandedChange = {},
                enabled = true,
                placeholder = {
                    Text(
                        "Buscar listas ou itens...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = null,
                colors = SearchBarDefaults.colors().inputFieldColors.copy(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                interactionSource = null,
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = SearchBarDefaults.colors(
            containerColor = Color.White,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        windowInsets = SearchBarDefaults.windowInsets,
    ) { }
}


private fun LazyGridScope.shoppingListContent(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
    if (uiState.isLoading) {
        items(LOADING_ITEMS_COUNT) {
            ShoppingItemLoading(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    } else if (uiState.filteredList.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ShoppingEmptyState()
        }
    } else {
        shoppingListItems(uiState, intent)
    }
}

private fun LazyGridScope.shoppingListItems(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
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
            ShoppingListItemWrapper(shopping, uiState, intent)
        }
    }
}

@Composable
private fun ShoppingListItemWrapper(
    shopping: Shopping,
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
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
