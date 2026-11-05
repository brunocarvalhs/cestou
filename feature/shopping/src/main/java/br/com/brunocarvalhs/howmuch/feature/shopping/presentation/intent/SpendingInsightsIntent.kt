package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryPurchase
import java.time.YearMonth

internal data class SpendingInsightsIntent(
    val onMonthSelected: (YearMonth) -> Unit = {},
    val onPurchaseClick: (HistoryPurchase) -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {}
)
