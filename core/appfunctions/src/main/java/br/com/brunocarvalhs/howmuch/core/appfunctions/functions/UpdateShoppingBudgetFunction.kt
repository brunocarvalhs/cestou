package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates the budget for a shopping list via AppFunctions.
 *
 * **Function Name:** `update_shopping_budget`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `newBudget`: New budget value
 *
 * **Returns:** Budget confirmation with remaining amount
 */
class UpdateShoppingBudgetFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun updateShoppingBudget(
        shoppingId: String,
        newBudget: Float,
    ): String = runCatching {
        // Validate input
        if (newBudget < 0) {
            trackInvocation(success = false, reason = "invalid_budget")
            return "Budget cannot be negative"
        }

        // Get shopping list
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        // Update budget
        val updatedShopping = shopping.copy(budget = newBudget.toDouble())
        shoppingRepository.update(updatedShopping)

        // Calculate spent
        val products = productsUseCase(shoppingId).first()
        val totalSpent = products.sumOf { it.total }
        val remaining = newBudget - totalSpent

        trackInvocation(success = true)
        "Budget updated to $${"%.2f".format(newBudget)} | " +
            "Spent: $${"%.2f".format(totalSpent)} | " +
            "Remaining: $${"%.2f".format(remaining)}"
    }.getOrElse { e ->
        Timber.e(e, "Error in updateShoppingBudget")
        trackInvocation(success = false, reason = "exception")
        "Failed to update budget: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "update_shopping_budget",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("update_shopping_budget failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
