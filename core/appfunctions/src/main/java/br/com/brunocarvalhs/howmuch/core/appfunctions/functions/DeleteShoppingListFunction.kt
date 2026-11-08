package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Deletes a shopping list via AppFunctions.
 *
 * **Function Name:** `delete_shopping_list`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list to delete
 *
 * **Returns:** Confirmation message or error description
 */
class DeleteShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun deleteShoppingList(shoppingId: String): String = runCatching {
        // Get and delete shopping list
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        shoppingRepository.delete(shopping)
        trackInvocation(success = true)
        "Shopping list '${shopping.title}' deleted successfully"
    }.getOrElse { e ->
        Timber.e(e, "Error in deleteShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to delete shopping list: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "delete_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("delete_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
