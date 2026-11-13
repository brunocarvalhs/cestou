package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.ShoppingDetailIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel
import kotlinx.coroutines.launch

private const val PAGE_TOTAL = 0
private const val PAGE_PRODUCTS = 1
private const val PAGE_BUDGET = 2
private const val PAGE_COUNT = 3

internal const val TAG_DETAIL_PAGER = "wear_detail_pager"
internal const val TAG_DETAIL_SEARCH_BUTTON = "wear_detail_search_button"
internal const val TAG_DETAIL_AI_BUTTON = "wear_detail_ai_button"
internal const val TAG_DETAIL_TOTAL = "wear_detail_total"
internal const val TAG_DETAIL_BUDGET_BAR = "wear_detail_budget_bar"

/**
 * Detalhe da lista em três visões horizontais: total, produtos e orçamento. A barra do topo
 * fica fixa: pesquisa (leva aos produtos e abre o filtro) e IA (abre o assistente).
 */
@Composable
internal fun ShoppingDetailPagerScreen(
    viewModel: ShoppingDetailViewModel,
    onNavigateToAi: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    ShoppingDetailPagerContent(
        state = state,
        intent = viewModel.intent,
        onNavigateToAi = onNavigateToAi
    )
}

@Composable
private fun ShoppingDetailPagerContent(
    state: ShoppingDetailUiState,
    intent: ShoppingDetailIntent,
    onNavigateToAi: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = PAGE_TOTAL, pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    val editing = editingId?.let { id -> state.items.items.firstOrNull { it.id == id } }
    if (editing != null) {
        ProductValueInput(
            product = editing,
            onSave = {
                intent.onSaveDetails(it)
                editingId = null
            },
            onCancel = { editingId = null }
        )
        return
    }

    ScreenScaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DetailActions(
                onSearch = {
                    searching = true
                    scope.launch { pagerState.animateScrollToPage(PAGE_PRODUCTS) }
                },
                onAi = onNavigateToAi
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(TAG_DETAIL_PAGER)
            ) { page ->
                when (page) {
                    PAGE_TOTAL -> TotalPage(state = state)
                    PAGE_PRODUCTS -> ProductsPage(
                        state = state,
                        intent = intent,
                        searching = searching,
                        query = query,
                        onQueryChange = { query = it },
                        onCloseSearch = {
                            searching = false
                            query = ""
                        },
                        onEditProduct = { editingId = it.id }
                    )
                    PAGE_BUDGET -> BudgetPage(state = state)
                }
            }

            HorizontalPageIndicator(pagerState = pagerState)
        }
    }
}

/** Pesquisa e IA, sempre no topo, acima das visões. */
@Composable
private fun DetailActions(onSearch: () -> Unit, onAi: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSearch,
            modifier = Modifier.testTag(TAG_DETAIL_SEARCH_BUTTON)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.shopping_wear_home_search),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = onAi,
            modifier = Modifier.testTag(TAG_DETAIL_AI_BUTTON)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = stringResource(R.string.shopping_wear_menu_ai),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Visão 1: o valor total da compra, em destaque. */
@Composable
private fun TotalPage(state: ShoppingDetailUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.shopping_wear_total),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = state.total.formatPrice(),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(TAG_DETAIL_TOTAL)
        )
        Text(
            text = if (state.pending == 0) {
                stringResource(R.string.shopping_wear_all_done)
            } else {
                pluralStringResource(R.plurals.shopping_wear_pending, state.pending, state.pending)
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/** Visão 2: produtos, com marcar, adicionar por voz e o filtro da pesquisa. */
@Composable
private fun ProductsPage(
    state: ShoppingDetailUiState,
    intent: ShoppingDetailIntent,
    searching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onEditProduct: (Product) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val openItemInput = rememberItemInput(intent.onAddItem)
    val visible = state.items.items.filter {
        query.isBlank() || it.name.contains(query.trim(), ignoreCase = true)
    }

    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        if (searching) {
            item {
                WearSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    hint = stringResource(R.string.shopping_wear_detail_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_SEARCH_FIELD)
                        .transformedHeight(this, spec)
                )
            }
            item {
                FilledTonalButton(
                    onClick = onCloseSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    label = { Text(stringResource(R.string.shopping_wear_detail_close_search)) }
                )
            }
        }

        item {
            FilledTonalButton(
                onClick = openItemInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wear_add_item")
                    .transformedHeight(this, spec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                transformation = SurfaceTransformation(spec),
                label = { Text(stringResource(R.string.shopping_wear_add_item)) }
            )
        }

        items(visible.size, key = { visible[it].id }) { index ->
            val product = visible[index]
            ProductButton(
                product = product,
                onClick = {
                    if (product.hasValue) intent.onToggle(product) else onEditProduct(product)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wear_item_${product.id}")
                    .transformedHeight(this, spec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                transformation = SurfaceTransformation(spec)
            )
        }

        if (visible.isEmpty() && !state.isLoading) {
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

/** Visão 3: orçamento contra o total gasto. Sem orçamento, avisa em vez de mostrar uma barra vazia. */
@Composable
private fun BudgetPage(state: ShoppingDetailUiState) {
    val budget = state.budget
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.shopping_wear_detail_budget_title),
            style = MaterialTheme.typography.labelMedium
        )
        if (budget == null || budget <= 0.0) {
            Text(
                text = stringResource(R.string.shopping_wear_detail_no_budget),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = budget.formatPrice(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            BudgetBar(
                fraction = (state.total / budget).toFloat().coerceIn(0f, 1f),
                over = state.total > budget
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.remaining?.let { remaining ->
                Text(
                    text = if (remaining < 0) {
                        stringResource(R.string.shopping_wear_over_budget, (-remaining).formatPrice())
                    } else {
                        stringResource(R.string.shopping_wear_remaining, remaining.formatPrice())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BudgetBar(fraction: Float, over: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag(TAG_DETAIL_BUDGET_BAR)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        )
    }
}

private val previewDetailProducts = listOf(
    Product(id = "1", name = "Arroz", quantity = 2.0, price = 25.0),
    Product(id = "2", name = "Feijão", quantity = 1.0, price = 8.5),
    Product(id = "3", name = "Café", quantity = 1.0, price = 15.0, isPurchased = true)
)

@WearPreviewDevices
@Composable
private fun ShoppingDetailPagerPreview() {
    MaterialTheme {
        ShoppingDetailPagerContent(
            state = ShoppingDetailUiState(
                title = "Café da manhã",
                emoji = "☕",
                total = 73.5,
                budget = 100.0,
                pending = 2,
                items = StableList(previewDetailProducts)
            ),
            intent = ShoppingDetailIntent(),
            onNavigateToAi = {}
        )
    }
}
