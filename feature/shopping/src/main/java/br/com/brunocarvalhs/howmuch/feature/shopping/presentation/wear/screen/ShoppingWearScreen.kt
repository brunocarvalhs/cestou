package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.ShoppingListViewModel

@Composable
internal fun ShoppingWearScreen(
    viewModel: ShoppingListViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    ShoppingWearContent(
        state = state,
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
private fun ShoppingWearContent(
    state: ShoppingListUiState,
    onNavigateToDetail: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    // Finished lists are history; the watch is for the shopping trip in progress.
    val lists = state.list.items.filter { it.status != Shopping.Status.FINISH }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.shopping_wear_my_lists))
                }
            }

            items(lists.size) { index ->
                val shopping = lists[index]
                Button(
                    onClick = { onNavigateToDetail(shopping.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("${shopping.emoji} ${shopping.title}".trim())
                }
            }

            if (lists.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = stringResource(R.string.shopping_wear_no_lists),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private val previewShoppingLists = listOf(
    Shopping(
        id = "1",
        title = "Café da manhã",
        description = "",
        price = 150.0,
        budget = 500.0,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("user1", "user2"),
        roles = emptyMap()
    ),
    Shopping(
        id = "2",
        title = "Jantar de domingo",
        description = "",
        price = 80.0,
        budget = 200.0,
        status = Shopping.Status.NEW,
        users = listOf("user3"),
        roles = emptyMap()
    )
)

@WearPreviewDevices
@Composable
private fun ShoppingWearContentPreview() {
    MaterialTheme {
        ShoppingWearContent(
            state = ShoppingListUiState(
                list = StableList(previewShoppingLists)
            ),
            onNavigateToDetail = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingWearContentLoadingPreview() {
    MaterialTheme {
        ShoppingWearContent(
            state = ShoppingListUiState(isLoading = true),
            onNavigateToDetail = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingWearContentEmptyPreview() {
    MaterialTheme {
        ShoppingWearContent(
            state = ShoppingListUiState(),
            onNavigateToDetail = {}
        )
    }
}
