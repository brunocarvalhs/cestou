package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.wear.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.ShoppingListUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.ShoppingListViewModel

@Composable
internal fun ShoppingWearScreen(
    viewModel: ShoppingListViewModel
) {
    val state by viewModel.uiState.collectAsState()
    ShoppingWearContent(state = state)
}

@Composable
private fun ShoppingWearContent(
    state: ShoppingListUiState
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

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
                    Text("Minhas Listas")
                }
            }

            items(state.list.items.size) { index ->
                val shopping = state.list.items[index]
                Button(
                    onClick = { /* Implement navigation to products */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(shopping.title)
                }
            }

            if (state.list.items.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = "Nenhuma lista encontrada",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
