package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouModalBottomSheet
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.common.ShoppingEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.CreateShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.NotificationBellButton
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.MonthlyGoalCard
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingFilterChips
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItemLoading
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingSearchBar
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState

private const val LOADING_ITEMS_COUNT = 5

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingScreen(
    uiState: ShoppingListUiState,
    windowSizeClass: WindowSizeClass,
    intent: ShoppingListIntent,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyGridState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it.asString(context)
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                intent.onFetchAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DragAndDropContainer {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(br.com.brunocarvalhs.howmuch.core.ui.R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        IconButton(onClick = intent.onShowJoinDialog, modifier = Modifier.testTag("home_join_button")) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(
                                    R.string.shopping_management_button_join
                                ),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        NotificationBellButton(
                            unreadCount = uiState.unreadNotifications,
                            onClick = intent.onNotificationsClick
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = intent.onCreate, modifier = Modifier.testTag("home_create_fab")) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(
                            R.string.shopping_management_content_description_create_list
                        )
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ShoppingSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = intent.onQueryChange,
                    onOpenAiChat = intent.onOpenAiChat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                ShoppingFilterChips(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = intent.onFilter,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                uiState.monthlyGoalProgress?.let { progress ->
                    MonthlyGoalCard(
                        progress = progress,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { intent.onFetchAll() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        shoppingListContent(uiState, intent)
                    }
                }
            }
        }

        if (uiState.isCreateSheetVisible) {
            CestouModalBottomSheet(
                onDismissRequest = { intent.onShowCreateSheet(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                CreateShoppingContent(
                    onConfirm = { title, description, emoji ->
                        intent.onCreateConfirmed(
                            title,
                            description,
                            emoji
                        )
                    },
                    onCancel = { intent.onShowCreateSheet(false) }
                )
            }
        }
    }
}

private fun LazyGridScope.shoppingListContent(
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
    if (uiState.isLoading) {
        items(LOADING_ITEMS_COUNT) {
            ShoppingItemLoading()
        }
    } else if (uiState.filteredList.isEmpty()) {
        item {
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
    items(uiState.filteredList, key = { it.id }) { shopping ->
        ShoppingListItemWrapper(shopping, uiState, intent)
    }
}

@Composable
private fun ShoppingListItemWrapper(
    shopping: Shopping,
    uiState: ShoppingListUiState,
    intent: ShoppingListIntent
) {
    ShoppingItem(
        modifier = Modifier.padding(vertical = 4.dp).testTag("home_list_item"),
        onClick = { intent.onOpen(shopping.id) },
        title = shopping.title,
        emoji = shopping.emoji,
        budget = shopping.budget,
        users = shopping.users,
        status = shopping.status,
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

private val previewShoppingLists = listOf(
    Shopping(
        id = "1",
        title = "Morning breakfast",
        description = "",
        price = 150.0,
        budget = 500.0,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("user1", "user2"),
        roles = emptyMap()
    ),
    Shopping(
        id = "2",
        title = "Dinner by Sarah",
        description = "",
        price = 80.0,
        budget = 200.0,
        status = Shopping.Status.NEW,
        users = listOf("user3"),
        roles = emptyMap()
    ),
    Shopping(
        id = "3",
        title = "Pizza day!",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = listOf("user1"),
        roles = emptyMap()
    )
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@PreviewCestouScreens
@Composable
private fun ShoppingPreview() {
    CestouTheme {
        ShoppingScreen(
            uiState = ShoppingListUiState(
                list = StableList(previewShoppingLists),
                filteredList = StableList(previewShoppingLists)
            ),
            windowSizeClass = rememberPreviewWindowSizeClass(),
            intent = ShoppingListIntent()
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@PreviewCestouScreens
@Composable
private fun ShoppingLoadingPreview() {
    CestouTheme {
        ShoppingScreen(
            uiState = ShoppingListUiState(isLoading = true),
            windowSizeClass = rememberPreviewWindowSizeClass(),
            intent = ShoppingListIntent()
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@PreviewCestouScreens
@Composable
private fun ShoppingEmptyPreview() {
    CestouTheme {
        ShoppingScreen(
            uiState = ShoppingListUiState(),
            windowSizeClass = rememberPreviewWindowSizeClass(),
            intent = ShoppingListIntent()
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
