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
 * Retrieves a summary of a shopping list including item count and total price via AppFunctions.
 *
 * **Function Name:** `get_shopping_list_summary`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list to summarize
 *
 * **Returns:** Text summary or error description
 */
class GetShoppingListSummaryFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun getShoppingListSummary(shoppingId: String): String = runCatching {
        // Get shopping list
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        // Get products
        val products = productsUseCase(shoppingId).first()

        // Calculate totals
        val itemCount = products.size
        val purchasedCount = products.count { it.isPurchased }
        val pendingCount = itemCount - purchasedCount
        val totalPrice = products.sumOf { it.total }
        val budget = shopping.budget ?: 0.0

        // Build summary
        val summaryParts = buildList {
            add("List: '${shopping.title}'")
            add("Items: $itemCount (pending: $pendingCount, purchased: $purchasedCount)")
            add("Total: $${"%.2f".format(totalPrice)}")
            if (budget > 0) {
                add("Budget: $${"%.2f".format(budget)}")
                val remaining = budget - totalPrice
                add("Remaining: $${"%.2f".format(remaining)}")
            }
        }

        trackInvocation(success = true)
        summaryParts.joinToString(" | ")
    }.getOrElse { e ->
        Timber.e(e, "Error in getShoppingListSummary")
        trackInvocation(success = false, reason = "exception")
        "Failed to get summary: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "get_shopping_list_summary",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("get_shopping_list_summary failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
