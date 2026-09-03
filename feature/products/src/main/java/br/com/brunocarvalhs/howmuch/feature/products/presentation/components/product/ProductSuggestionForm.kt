package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.presentation.event.ProductSuggestionEvent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductSuggestionIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductSuggestionUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun ProductSuggestionForm(
    uiState: ProductSuggestionUiState = ProductSuggestionUiState(),
    events: Flow<ProductSuggestionEvent>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = { },
    intent: ProductSuggestionIntent = ProductSuggestionIntent()
) {
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                ProductSuggestionEvent.ProductAdded -> onBack()
                is ProductSuggestionEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val filteredProducts = remember(uiState.suggestions, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.suggestions
        } else {
            uiState.suggestions.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ProductSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { intent.onSearchProduct(it) }
        )

        if (filteredProducts.isEmpty()) {
            ProductEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = filteredProducts,
                    key = { it.id }
                ) { product ->
                    ProductItem(
                        name = product.name,
                        onClick = { intent.onAddProduct(product) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}

private const val PREVIEW_PRICE_ARROZ = 10.0
private const val PREVIEW_PRICE_FEIJAO = 8.0

@Preview(showBackground = true)
@Composable
private fun ProductSuggestionFormPreview() {
    ProductSuggestionForm(
        uiState = ProductSuggestionUiState(
            suggestions = listOf(
                Product("1", "Arroz", 1.0, PREVIEW_PRICE_ARROZ),
                Product("2", "Feijão", 1.0, PREVIEW_PRICE_FEIJAO)
            )
        ),
        events = emptyFlow()
    )
}

@Preview(showBackground = true)
@Composable
private fun ProductSuggestionFormEmptyPreview() {
    ProductSuggestionForm(
        uiState = ProductSuggestionUiState(),
        events = emptyFlow()
    )
}
