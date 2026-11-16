package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButtonStyle
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouHeaderButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouModalBottomSheet
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.common.ShoppingEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.CreateShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.NotificationBellButton
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.FeaturedItemsSection
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.FeaturedListCard
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItemLoading
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
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currencyFormatter = rememberCurrencyFormatter()

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
                            text = stringResource(R.string.shopping_home_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        CestouHeaderButton(
                            onClick = intent.onShowJoinDialog,
                            contentDescription = stringResource(R.string.shopping_management_button_join),
                            testTag = "home_join_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        NotificationBellButton(
                            unreadCount = uiState.unreadNotifications,
                            onClick = intent.onNotificationsClick
                        )
                        CestouHeaderButton(
                            onClick = intent.onCreate,
                            contentDescription = stringResource(
                                R.string.shopping_management_content_description_create_list
                            ),
                            testTag = "home_create_fab",
                            filled = true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            val featured = uiState.list.firstOrNull { it.status == Shopping.Status.IN_PROGRESS }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { intent.onFetchAll() },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        featured?.let { shopping ->
                            item(key = "featured_card") {
                                FeaturedListCard(
                                    shopping = shopping,
                                    currencyFormatter = currencyFormatter,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                            item(key = "featured_items") {
                                FeaturedItemsSection(
                                    items = uiState.featuredItems.items,
                                    users = shopping.users,
                                    currencyFormatter = currencyFormatter,
                                    onOpen = { intent.onOpen(shopping.id) },
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                        shoppingListContent(uiState, intent)
                    }
                }

                featured?.let { shopping ->
                    CestouButton(
                        text = stringResource(R.string.shopping_featured_continue),
                        onClick = { intent.onOpen(shopping.id) },
                        style = CestouButtonStyle.Strong,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 20.dp)
                    )
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

private fun LazyListScope.shoppingListContent(
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

private fun LazyListScope.shoppingListItems(
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
