package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Reopens a closed shopping list via AppFunctions.
 *
 * **Function Name:** `reopen_shopping_list`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list to reopen
 *
 * **Returns:** Confirmation message or error
 */
class ReopenShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun reopenShoppingList(shoppingId: String): String = runCatching {
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        if (shopping.status != Shopping.Status.FINISH) {
            trackInvocation(success = false, reason = "already_active")
            return "Shopping list is already active"
        }

        val reopened = shopping.copy(status = Shopping.Status.NEW)
        shoppingRepository.update(reopened)

        trackInvocation(success = true)
        "Shopping list '${shopping.title}' reopened successfully"
    }.getOrElse { e ->
        Timber.e(e, "Error in reopenShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to reopen list: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "reopen_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("reopen_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
