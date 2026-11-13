package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.contract.LocalAdBanner
import br.com.brunocarvalhs.howmuch.core.ui.model.AdPlacement
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryEmptyState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryErrorState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryMonthCarousel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.HistoryPurchaseRow
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.fullLabel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.HistoryIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.HistoryUiState

/** Stateless: the caller owns the ViewModel, so this is what tests and previews render. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    state: HistoryUiState,
    intent: HistoryIntent,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("history_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    if (state.months.isNotEmpty()) {
                        TextButton(
                            onClick = intent.onSeeAnalysis,
                            modifier = Modifier.testTag("history_see_analysis")
                        ) {
                            Text(text = stringResource(R.string.history_see_analysis))
                        }
                    }
                }
            )
        },
        // Banner só para FREE; sem provedor de anúncios (ex.: :wear) não ocupa espaço.
        bottomBar = { LocalAdBanner.current(AdPlacement.HISTORY, Modifier) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).testTag("history_loading")
                )
                state.error != null -> HistoryErrorState(
                    message = state.error.asString(),
                    retryLabel = stringResource(R.string.history_retry),
                    errorTag = "history_error",
                    retryTag = "history_retry",
                    onRetry = intent.onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.isEmpty -> HistoryEmptyState(
                    title = stringResource(R.string.history_empty_title),
                    hint = stringResource(R.string.history_empty_hint),
                    tag = "history_empty",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> HistoryContent(state = state, intent = intent)
            }
        }
    }
}

@Composable
private fun HistoryContent(state: HistoryUiState, intent: HistoryIntent) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "carousel", contentType = "carousel") {
            HistoryMonthCarousel(
                months = state.months,
                selectedMonth = state.selectedMonth,
                onMonthSelect = intent.onMonthSelected
            )
        }
        state.selectedMonth?.let { month ->
            item(key = "title", contentType = "title") {
                Text(
                    text = stringResource(R.string.history_month_purchases, month.fullLabel()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        items(state.selectedPurchases, key = { it.shopping.id }, contentType = { "purchase" }) { purchase ->
            HistoryPurchaseRow(
                purchase = purchase,
                onClick = { intent.onPurchaseClick(purchase) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@PreviewCestouScreens
@Composable
private fun HistoryScreenEmptyPreview() {
    CestouTheme {
        HistoryScreen(state = HistoryUiState(isLoading = false), intent = HistoryIntent())
    }
}
