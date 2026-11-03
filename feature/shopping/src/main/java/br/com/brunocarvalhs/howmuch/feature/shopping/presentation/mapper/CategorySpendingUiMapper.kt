package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.mapper

import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.CategorySpending
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.CategoryShare

private const val TOP_CATEGORIES = 5

/**
 * Turns the raw category keys of the domain into the ranking the UI draws: keys that resolve to the
 * same [ProductCategory] ("Dairy" and "Laticínios") are merged, the [top] largest stay and the rest
 * collapse into one "others" entry (`category == null`). Percentages are over the sum of [spending].
 */
internal fun List<CategorySpending>.toCategoryShares(top: Int = TOP_CATEGORIES): List<CategoryShare> {
    val grand = sumOf { it.total }
    if (grand <= 0.0) return emptyList()

    val merged = groupBy { ProductCategory.fromString(it.rawKey) }
        .map { (category, items) -> category to items.sumOf { it.total } }
        .sortedWith(compareByDescending<Pair<ProductCategory, Double>> { it.second }.thenBy { it.first.ordinal })

    val shares = merged.take(top).map { (category, total) -> CategoryShare(category, total, (total / grand).toFloat()) }
    val rest = merged.drop(top).sumOf { it.second }
    return if (rest > 0.0) shares + CategoryShare(null, rest, (rest / grand).toFloat()) else shares
}
