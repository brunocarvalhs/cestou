package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping

/**
 * Best-effort purchase date for a finished list: the latest `PURCHASED` activity among
 * [products], falling back to [Shopping.createdAt] when none exists. A single call site, so
 * preferring a dedicated finish timestamp once one exists on [Shopping] is a one-line change
 * here instead of a wider rewrite.
 */
internal fun Shopping.resolvedPurchaseDate(products: List<Product>): Long =
    products
        .asSequence()
        .flatMap { it.history }
        .filter { it.action == ProductActivity.Action.PURCHASED }
        .maxOfOrNull { it.timestamp }
        ?: createdAt

/** Resolved total paid for a finished list, and whether it had to be calculated from items. */
internal data class ResolvedPurchaseTotal(val amount: Double, val isEstimated: Boolean)

/**
 * [Shopping.price] when confirmed (`> 0`), otherwise the sum of purchased items' totals, flagged
 * as estimated so a calculated number is never confused with a user-confirmed one.
 */
internal fun Shopping.resolvedPurchaseTotal(products: List<Product>): ResolvedPurchaseTotal =
    if (price > 0) {
        ResolvedPurchaseTotal(amount = price, isEstimated = false)
    } else {
        ResolvedPurchaseTotal(
            amount = products.filter { it.isPurchased }.sumOf { it.total },
            isEstimated = true
        )
    }
