package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryErrorState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryPurchaseRow
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.fullLabel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.insights.CategoryRankingRow
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.insights.MonthlySpendingChart
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.SpendingInsightsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendingInsightsUiState

/** Stateless: the caller owns the ViewModel, so this is what tests and previews render. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpendingInsightsScreen(
    state: SpendingInsightsUiState,
    intent: SpendingInsightsIntent,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("insights_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.insights_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = intent.onBack, modifier = Modifier.testTag("insights_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.insights_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).testTag("insights_loading")
                )
                state.error != null -> HistoryErrorState(
                    message = state.error.asString(),
                    retryLabel = stringResource(R.string.insights_retry),
                    errorTag = "insights_error",
                    retryTag = "insights_retry",
                    onRetry = intent.onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.isEmpty -> HistoryEmptyState(
                    title = stringResource(R.string.insights_empty_title),
                    hint = stringResource(R.string.insights_empty_hint),
                    tag = "insights_empty",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> InsightsContent(state = state, intent = intent)
            }
        }
    }
}

@Composable
private fun InsightsContent(state: SpendingInsightsUiState, intent: SpendingInsightsIntent) {
    val monthLabel = state.selectedMonth?.fullLabel().orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "chart", contentType = "chart") {
            CestouCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(stringResource(R.string.insights_total_per_month))
                    Text(
                        text = state.selectedTotal.formatPrice(LocalCurrency.current),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    MonthlySpendingChart(
                        months = state.months,
                        selectedMonth = state.selectedMonth,
                        onMonthSelect = intent.onMonthSelected
                    )
                }
            }
        }
        categoriesSection(state, intent, monthLabel)
        item(key = "purchases-title", contentType = "title") {
            SectionTitle(stringResource(R.string.insights_purchases_of, monthLabel))
        }
        items(state.purchases.items, key = { it.shopping.id }, contentType = { "purchase" }) { purchase ->
            HistoryPurchaseRow(
                purchase = purchase,
                onClick = { intent.onPurchaseClick(purchase) },
                testTagPrefix = "insights_purchase_"
            )
        }
    }
}

private fun LazyListScope.categoriesSection(
    state: SpendingInsightsUiState,
    intent: SpendingInsightsIntent,
    monthLabel: String
) {
    item(key = "categories-title", contentType = "title") {
        SectionTitle(stringResource(R.string.insights_top_categories, monthLabel))
    }
    when {
        state.categoriesLoading -> item(key = "categories-loading", contentType = "state") {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTag("insights_categories_loading"))
            }
        }
        state.categoriesError != null -> item(key = "categories-error", contentType = "state") {
            HistoryErrorState(
                message = state.categoriesError.asString(),
                retryLabel = stringResource(R.string.insights_retry),
                errorTag = "insights_categories_error",
                retryTag = "insights_categories_retry",
                onRetry = intent.onRetry
            )
        }
        state.categories.isEmpty() -> item(key = "categories-empty", contentType = "state") {
            Text(
                text = stringResource(R.string.insights_categories_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("insights_categories_empty")
            )
        }
        else -> {
            itemsIndexed(
                state.categories.items,
                key = { index, _ -> "category-$index" },
                contentType = { _, _ -> "category" }
            ) { index, share ->
                CategoryRankingRow(share = share, index = index)
            }
            if (state.categoriesDiverge) {
                item(key = "categories-footer", contentType = "state") {
                    Text(
                        text = stringResource(R.string.insights_categories_footer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
}

@PreviewCestouScreens
@Composable
private fun SpendingInsightsEmptyPreview() {
    CestouTheme {
        SpendingInsightsScreen(state = SpendingInsightsUiState(isLoading = false), intent = SpendingInsightsIntent())
    }
}
