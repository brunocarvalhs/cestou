package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragTarget
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DropTarget
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.common.ShoppingEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.form.CreateShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping.ShoppingItem
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping.ShoppingItemLoading
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.ShoppingListUiState

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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyGridState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it.asString(context)
            )
        }
    }

    LaunchedEffect(Unit) {
        intent.onFetchAll()
    }

    DragAndDropContainer {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    actions = {
                        IconButton(onClick = intent.onCreate) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Shopping lists",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        SectionHeader("Our recommendations")
                    }
                    item {
                        ShoppingItem(
                            title = "Often purchased",
                            description = "",
                            price = 0.0,
                            itemCount = 10,
                            iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            onClick = { /* Recommendation click */ }
                        )
                    }

                    item {
                        SectionHeader("Recently created")
                    }

                    shoppingListContent(uiState, intent)
                }
            }
        }

        if (uiState.isCreateSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { intent.onShowCreateSheet(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CreateShoppingContent(
                    onConfirm = { title, description ->
                        intent.onCreateConfirmed(
                            title,
                            description
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
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
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { intent.onOpen(shopping.id) },
                title = shopping.title,
                description = shopping.description,
                price = shopping.price,
                budget = shopping.budget,
                itemCount = 8,
                users = shopping.users,
                status = shopping.status,
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
private fun ShoppingPreview() {
    CestouTheme {
        val list = listOf(
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

        ShoppingScreen(
            uiState = ShoppingListUiState(
                list = StableList(list),
                filteredList = StableList(list)
            ),
            windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
            intent = ShoppingListIntent()
        )
    }
}
