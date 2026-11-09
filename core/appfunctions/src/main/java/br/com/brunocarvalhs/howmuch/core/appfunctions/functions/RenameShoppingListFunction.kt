package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Renames a shopping list via AppFunctions.
 *
 * **Function Name:** `rename_shopping_list`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `newTitle`: New title for the list
 *
 * **Returns:** Confirmation with new title
 */
class RenameShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun renameShoppingList(shoppingId: String, newTitle: String): String = runCatching {
        if (newTitle.isBlank()) {
            trackInvocation(success = false, reason = "empty_title")
            return "Title cannot be empty"
        }

        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        val updated = shopping.copy(title = newTitle)
        shoppingRepository.update(updated)

        trackInvocation(success = true)
        "List renamed to '$newTitle'"
    }.getOrElse { e ->
        Timber.e(e, "Error in renameShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to rename list: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "rename_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("rename_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
