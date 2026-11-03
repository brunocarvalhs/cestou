package br.com.brunocarvalhs.howmuch.feature.shopping.domain.model

import java.time.YearMonth

internal data class MonthSpending(
    val key: YearMonth,
    val total: Double,
    val purchaseCount: Int
)

/**
 * [rawKey] is the product category as stored (trimmed and lowercased). Mapping it to the
 * `ProductCategory` enum lives in presentation because that enum depends on Compose.
 */
internal data class CategorySpending(
    val rawKey: String,
    val total: Double
)

internal fun HistoryMonth.toSpending() = MonthSpending(
    key = key,
    total = total,
    purchaseCount = purchases.size
)
