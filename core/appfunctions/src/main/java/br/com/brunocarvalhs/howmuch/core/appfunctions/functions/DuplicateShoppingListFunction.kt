package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Duplicates a shopping list with all its items via AppFunctions.
 *
 * **Function Name:** `duplicate_shopping_list`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list to duplicate
 * - `newTitle`: New title (optional, defaults to original + " (Copy)")
 *
 * **Returns:** ID and title of the new list
 */
class DuplicateShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun duplicateShoppingList(shoppingId: String, newTitle: String? = null): String = runCatching {
        val original = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        val title = newTitle ?: "${original.title} (Copy)"
        val duplicate = original.copy(
            id = "temp-${System.currentTimeMillis()}",
            title = title
        )

        shoppingRepository.create(duplicate)

        trackInvocation(success = true)
        "List duplicated to '$title'"
    }.getOrElse { e ->
        Timber.e(e, "Error in duplicateShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to duplicate list: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "duplicate_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("duplicate_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
