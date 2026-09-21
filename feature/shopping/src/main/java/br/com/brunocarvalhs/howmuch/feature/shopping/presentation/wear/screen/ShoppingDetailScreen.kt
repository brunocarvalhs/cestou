package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel

@Composable
internal fun ShoppingDetailScreen(
    viewModel: ShoppingDetailViewModel
) {
    val state by viewModel.uiState.collectAsState()
    ShoppingDetailContent(
        state = state,
        onBack = { viewModel.intent.onBack() }
    )
}

@Composable
private fun ShoppingDetailContent(
    state: ShoppingDetailUiState,
    onBack: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(
                        text = state.title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                TitleCard(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Text(
                            text = "Saldo",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                ) {
                    Text(
                        text = state.balance.formatPrice(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (state.balance < 0) Color.Red else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Orçamento: ${state.budget.formatPrice()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    Text("Produtos")
                }
            }

            items(state.items.items.size) { index ->
                val product = state.items.items[index]
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = {
                        Text(
                            text = product.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Row {
                            Text(
                                text = "${product.quantity.toInt()}x ${product.price?.formatPrice() ?: ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }

            if (state.items.items.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = "Nenhum produto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private val previewProducts = listOf(
    Product(id = "1", name = "Arroz", quantity = 2.0, price = 25.0),
    Product(id = "2", name = "Feijão", quantity = 1.0, price = 8.5),
    Product(id = "3", name = "Café", quantity = 1.0, price = 15.0)
)

@WearPreviewDevices
@Composable
private fun ShoppingDetailContentPreview() {
    MaterialTheme {
        ShoppingDetailContent(
            state = ShoppingDetailUiState(
                title = "Café da manhã",
                budget = 500.0,
                totalSpent = 48.5,
                balance = 451.5,
                items = StableList(previewProducts)
            ),
            onBack = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingDetailContentNegativeBalancePreview() {
    MaterialTheme {
        ShoppingDetailContent(
            state = ShoppingDetailUiState(
                title = "Jantar de domingo",
                budget = 100.0,
                totalSpent = 130.0,
                balance = -30.0,
                items = StableList(previewProducts)
            ),
            onBack = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingDetailContentEmptyPreview() {
    MaterialTheme {
        ShoppingDetailContent(
            state = ShoppingDetailUiState(
                title = "Pizza day!",
                budget = 0.0,
                totalSpent = 0.0,
                balance = 0.0
            ),
            onBack = {}
        )
    }
}
