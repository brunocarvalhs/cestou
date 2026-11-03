package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryPurchase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.MonthSpending
import java.time.YearMonth

/** One ranking entry; `category == null` is the aggregated "others" bucket. */
@Immutable
internal data class CategoryShare(
    val category: ProductCategory?,
    val total: Double,
    val percent: Float
)

@Immutable
internal data class SpendingInsightsUiState(
    val isLoading: Boolean = true,
    val months: StableList<MonthSpending> = StableList(),
    val selectedMonth: YearMonth? = null,
    val categories: StableList<CategoryShare> = StableList(),
    val categoriesLoading: Boolean = false,
    val categoriesError: UiText? = null,
    /** True when the products sum differs from the month's `Shopping.price` total (rule 4). */
    val categoriesDiverge: Boolean = false,
    val purchases: StableList<HistoryPurchase> = StableList(),
    val error: UiText? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && months.isEmpty()

    val selectedTotal: Double
        get() = months.firstOrNull { it.key == selectedMonth }?.total ?: 0.0
}
