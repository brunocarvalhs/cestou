package br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTopBar
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistoryDateHeader
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistoryEmptyMonthState
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistoryErrorState
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistoryLoadingState
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistoryMonthCardRow
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistoryPurchaseListItem
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory.SpendHistorySummaryHeader
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.InProgressSpend
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.MonthBudget
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.MonthSpendSummary
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.SpendHistoryPurchase
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.SpendHistoryUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

/**
 * "Histórico de gastos" (spend history) screen — presentation-only, driven entirely by
 * [SpendHistoryUiState]. No ViewModel/repository wiring yet: `.specs/features/
 * purchase-history-export/spec.md` is still draft, so this previews against fake data. See the
 * KDoc on [SpendHistoryUiState] for the provisional-module-placement note.
 *
 * Deviations from bruno's literal reference layout (flagged for `pm`/bruno to reconcile):
 * - No help ("?") icon in the top bar — the same-day data spec explicitly cut it ("no equivalent
 *   need in this domain"), and a non-functional help icon would be worse than none.
 * - No "Filtros" affordance next to the purchases list — the data spec defers filters past v1
 *   ("typically a handful of entries"), and a visible-but-inert filter button would be a dead
 *   end for anyone who taps it. Both can be added back the moment there's real behavior behind
 *   them.
 * - The reference's generic "transactions" list is labeled "Compras" (purchases), not
 *   "Lançamentos" — each row is one finished shopping list, not a generic ledger entry, and
 *   "Compras" is what the spec's own acceptance criteria settled on.
 *
 * [onExportClick] is a plain hook — this screen never reads `SubscriptionStatus` itself. Per the
 * spec's scope boundary, every row here (cards, trend line, purchases list) is free; only the
 * export action it wires to is Pro-gated, and that gating happens at the call site, not here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpendHistoryScreen(
    uiState: SpendHistoryUiState,
    onBack: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onMonthSelected: (String) -> Unit = {},
    onPurchaseClick: (SpendHistoryPurchase) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var selectedMonthId by rememberSaveable { mutableStateOf(uiState.selectedMonthId) }

    LaunchedEffect(uiState.months) {
        if (uiState.months.none { it.id == selectedMonthId }) {
            selectedMonthId = uiState.months.firstOrNull { it.isCurrent }?.id
                ?: uiState.months.lastOrNull()?.id
        }
    }

    Scaffold(
        topBar = {
            CestouTopBar(
                title = { Text(stringResource(R.string.spend_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.content_description_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onExportClick,
                        modifier = Modifier.testTag("spend_history_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.spend_history_export_action)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> SpendHistoryLoadingState()
                uiState.error != null -> SpendHistoryErrorState(
                    message = uiState.error.asString(),
                    onRetry = onRetry
                )

                uiState.months.isEmpty() -> SpendHistoryEmptyMonthState(
                    modifier = Modifier.fillMaxSize(),
                    monthLabel = null
                )

                else -> {
                    val selectedMonth = uiState.months.firstOrNull { it.id == selectedMonthId }
                        ?: uiState.months.last()

                    Column(modifier = Modifier.fillMaxSize()) {
                        SpendHistoryMonthCardRow(
                            months = uiState.months,
                            selectedMonthId = selectedMonth.id,
                            onMonthSelected = { id ->
                                selectedMonthId = id
                                onMonthSelected(id)
                            }
                        )
                        SpendHistorySummaryHeader(month = selectedMonth)
                        HorizontalDivider()
                        SpendHistoryPurchasesSection(
                            month = selectedMonth,
                            onPurchaseClick = onPurchaseClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendHistoryPurchasesSection(
    month: MonthSpendSummary,
    onPurchaseClick: (SpendHistoryPurchase) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.spend_history_purchases_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (month.purchases.isEmpty()) {
            SpendHistoryEmptyMonthState(
                modifier = Modifier.fillMaxWidth(),
                monthLabel = month.fullLabel
            )
        } else {
            val grouped = month.purchases
                .sortedByDescending { it.timestamp }
                .groupBy { dayBucket(it.timestamp) }
                .toList()

            LazyColumn {
                grouped.forEach { (dayKey, purchasesOfDay) ->
                    item(key = "header_$dayKey") {
                        SpendHistoryDateHeader(timestamp = purchasesOfDay.first().timestamp)
                    }
                    itemsIndexed(
                        purchasesOfDay,
                        key = { _, purchase -> purchase.id }
                    ) { index, purchase ->
                        SpendHistoryPurchaseListItem(
                            purchase = purchase,
                            onClick = { onPurchaseClick(purchase) }
                        )
                        if (index < purchasesOfDay.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun dayBucket(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

// region Preview data

private fun previewTimestamp(daysAgo: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return calendar.timeInMillis
}

private val previewJunePurchases = StableList(
    listOf(
        SpendHistoryPurchase(
            id = "p1",
            timestamp = previewTimestamp(1),
            emoji = "🛒",
            title = "Compras do mês",
            establishment = "Supermercado Extra",
            total = 24.60,
            isShared = true
        ),
        SpendHistoryPurchase(
            id = "p2",
            timestamp = previewTimestamp(5),
            emoji = "🧺",
            title = "Padaria rápida",
            establishment = null,
            total = 18.30,
            isEstimated = true
        )
    )
)

private val previewMonths = StableList(
    listOf(
        MonthSpendSummary(id = "2026-03", monthLabel = "Mar", fullLabel = "Março", total = 26.58),
        MonthSpendSummary(id = "2026-04", monthLabel = "Abr", fullLabel = "Abril", total = 70.63),
        MonthSpendSummary(id = "2026-05", monthLabel = "Mai", fullLabel = "Maio", total = 76.12),
        MonthSpendSummary(
            id = "2026-06",
            monthLabel = "Jun",
            fullLabel = "Junho",
            total = previewJunePurchases.sumOf { it.total },
            isCurrent = true,
            budget = MonthBudget(amount = 300.0, spent = 320.0),
            inProgress = InProgressSpend(total = 12.0, listCount = 1),
            purchases = previewJunePurchases
        )
    )
)

@PreviewCestouScreens
@Composable
private fun SpendHistoryScreenPreview() {
    CestouTheme {
        SpendHistoryScreen(
            uiState = SpendHistoryUiState(months = previewMonths, selectedMonthId = "2026-06")
        )
    }
}

@PreviewCestouScreens
@Composable
private fun SpendHistoryScreenEmptyMonthPreview() {
    CestouTheme {
        SpendHistoryScreen(
            uiState = SpendHistoryUiState(
                months = StableList(
                    previewMonths.map { month ->
                        if (month.isCurrent) {
                            month.copy(
                                total = 0.0,
                                purchases = StableList(),
                                budget = null,
                                inProgress = null
                            )
                        } else {
                            month
                        }
                    }
                ),
                selectedMonthId = "2026-06"
            )
        )
    }
}

@PreviewCestouScreens
@Composable
private fun SpendHistoryScreenFirstEverUsePreview() {
    CestouTheme {
        SpendHistoryScreen(
            uiState = SpendHistoryUiState(
                months = StableList(listOf(previewMonths.last())),
                selectedMonthId = "2026-06"
            )
        )
    }
}

@PreviewCestouScreens
@Composable
private fun SpendHistoryScreenLoadingPreview() {
    CestouTheme {
        SpendHistoryScreen(uiState = SpendHistoryUiState(isLoading = true))
    }
}

@PreviewCestouScreens
@Composable
private fun SpendHistoryScreenErrorPreview() {
    CestouTheme {
        SpendHistoryScreen(
            uiState = SpendHistoryUiState(
                error = UiText.StringResource(R.string.spend_history_error_message)
            )
        )
    }
}

// endregion
