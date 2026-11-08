package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Gets spending breakdown by category via AppFunctions.
 *
 * **Function Name:** `get_category_spending`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 *
 * **Returns:** Breakdown of spending per category
 */
class GetCategorySpendingFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    companion object {
        private const val PERCENTAGE_MULTIPLIER = 100
    }

    suspend fun getCategorySpending(shoppingId: String): String = runCatching {
        // Get shopping list
        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        // Get products
        val products = productsUseCase(shoppingId).first()

        if (products.isEmpty()) {
            trackInvocation(success = true)
            return "No products in this list"
        }

        // Group by category and sum
        val byCategory = products
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.total } }
            .toSortedMap()

        val total = byCategory.values.sum()
        val breakdown = byCategory.entries.map { (category, amount) ->
            val percentage = (amount / total * PERCENTAGE_MULTIPLIER).toInt()
            "$category: $${"%.2f".format(amount)} ($percentage%)"
        }.joinToString("\n")

        trackInvocation(success = true)
        "Spending by category:\n$breakdown\n\nTotal: $${"%.2f".format(total)}"
    }.getOrElse { e ->
        Timber.e(e, "Error in getCategorySpending")
        trackInvocation(success = false, reason = "exception")
        "Failed to get category spending: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "get_category_spending",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("get_category_spending failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
