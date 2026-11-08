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
 * Gets detailed information about a shopping list via AppFunctions.
 *
 * **Function Name:** `get_shopping_details`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 *
 * **Returns:** Full details including budget, spent, item count, members
 */
class GetShoppingDetailsFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun getShoppingDetails(shoppingId: String): String = runCatching {
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        val products = productsUseCase(shoppingId).first()
        val totalSpent = products.sumOf { it.total }
        val memberCount = shopping.users.size
        val budget = shopping.budget ?: 0.0
        val remaining = budget - totalSpent

        trackInvocation(success = true)
        "List: ${shopping.title}\n" +
            "Budget: $${"%.2f".format(budget)}\n" +
            "Spent: $${"%.2f".format(totalSpent)}\n" +
            "Remaining: $${"%.2f".format(remaining)}\n" +
            "Items: ${products.size}\n" +
            "Members: $memberCount"
    }.getOrElse { e ->
        Timber.e(e, "Error in getShoppingDetails")
        trackInvocation(success = false, reason = "exception")
        "Failed to get shopping details: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "get_shopping_details",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("get_shopping_details failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
