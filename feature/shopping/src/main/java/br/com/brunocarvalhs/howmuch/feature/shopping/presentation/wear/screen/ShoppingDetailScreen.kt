package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import android.app.RemoteInput
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.FilledTonalButton
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
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.ShoppingDetailIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel

private const val ITEM_INPUT_KEY = "item_name"

@Composable
internal fun ShoppingDetailScreen(
    viewModel: ShoppingDetailViewModel
) {
    val state by viewModel.uiState.collectAsState()
    ShoppingDetailContent(state = state, intent = viewModel.intent)
}

/** Opens the system text input, which offers both voice and keyboard on Wear OS. */
@Composable
private fun rememberItemInput(onText: (String) -> Unit): () -> Unit {
    val label = stringResource(R.string.shopping_wear_add_item_hint)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data
            ?.let { RemoteInput.getResultsFromIntent(it) }
            ?.getCharSequence(ITEM_INPUT_KEY)
            ?.let { onText(it.toString()) }
    }
    return {
        val remoteInput = RemoteInput.Builder(ITEM_INPUT_KEY)
            .setLabel(label)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }
            .build()
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        launcher.launch(intent)
    }
}

@Composable
private fun ShoppingDetailContent(
    state: ShoppingDetailUiState,
    intent: ShoppingDetailIntent
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val openItemInput = rememberItemInput(intent.onAddItem)

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
                        text = "${state.emoji} ${state.title}".trim(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                TitleCard(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wear_list_summary")
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Text(
                            text = stringResource(R.string.shopping_wear_total),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                ) {
                    Text(text = state.total.formatPrice(), style = MaterialTheme.typography.titleLarge)
                    state.remaining?.let { remaining ->
                        Text(
                            text = if (remaining < 0) {
                                stringResource(R.string.shopping_wear_over_budget, (-remaining).formatPrice())
                            } else {
                                stringResource(R.string.shopping_wear_remaining, remaining.formatPrice())
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remaining < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Text(
                        text = if (state.pending == 0) {
                            stringResource(R.string.shopping_wear_all_done)
                        } else {
                            pluralStringResource(R.plurals.shopping_wear_pending, state.pending, state.pending)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = openItemInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wear_add_item")
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = { Text(stringResource(R.string.shopping_wear_add_item)) }
                )
            }

            items(state.items.items.size) { index ->
                val product = state.items.items[index]
                ProductButton(
                    product = product,
                    onClick = { intent.onToggle(product) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wear_item_${product.id}")
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            if (state.items.items.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = stringResource(R.string.shopping_wear_no_products),
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

@Composable
private fun ProductButton(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation
) {
    val decoration = if (product.isPurchased) TextDecoration.LineThrough else null
    val details = buildString {
        append("${product.quantity.toInt()}x")
        product.price?.let { append(" · ${it.formatPrice()}") }
    }
    val colors = if (product.isPurchased) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
    Button(
        onClick = onClick,
        modifier = modifier,
        transformation = transformation,
        colors = colors,
        label = {
            Text(
                text = if (product.isPurchased) "✓ ${product.name}" else product.name,
                textDecoration = decoration,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(text = details, style = MaterialTheme.typography.bodySmall, textDecoration = decoration)
        }
    )
}

private val previewProducts = listOf(
    Product(id = "1", name = "Arroz", quantity = 2.0, price = 25.0),
    Product(id = "2", name = "Feijão", quantity = 1.0, price = 8.5),
    Product(id = "3", name = "Café", quantity = 1.0, price = 15.0, isPurchased = true)
)

@WearPreviewDevices
@Composable
private fun ShoppingDetailContentPreview() {
    MaterialTheme {
        ShoppingDetailContent(
            state = ShoppingDetailUiState(
                title = "Café da manhã",
                emoji = "☕",
                total = 73.5,
                budget = 500.0,
                pending = 2,
                items = StableList(previewProducts)
            ),
            intent = ShoppingDetailIntent()
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingDetailContentOverBudgetPreview() {
    MaterialTheme {
        ShoppingDetailContent(
            state = ShoppingDetailUiState(
                title = "Jantar de domingo",
                total = 130.0,
                budget = 100.0,
                pending = 1,
                items = StableList(previewProducts)
            ),
            intent = ShoppingDetailIntent()
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingDetailContentEmptyPreview() {
    MaterialTheme {
        ShoppingDetailContent(
            state = ShoppingDetailUiState(title = "Pizza day!"),
            intent = ShoppingDetailIntent()
        )
    }
}
