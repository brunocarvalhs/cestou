package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Joins a shared shopping list using a short code via AppFunctions.
 *
 * **Function Name:** `join_shopping_list`
 *
 * **Parameters:**
 * - `shortCode`: Shareable code of the shopping list (e.g., "ABC123")
 *
 * **Returns:** List ID and name, or error message
 */
class JoinShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun joinShoppingList(shortCode: String): String = runCatching {
        // Validate input
        if (shortCode.isBlank()) {
            trackInvocation(success = false, reason = "blank_code")
            return "Share code cannot be empty"
        }

        // Resolve short code to list ID
        val shoppingId = shoppingRepository.resolveShortCode(shortCode)
            ?: return run {
                trackInvocation(success = false, reason = "invalid_code")
                "Invalid share code: $shortCode"
            }

        // Get shopping list info
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "list_not_found")
                "List not found after resolving code"
            }

        // Try to join
        try {
            shoppingRepository.join(shoppingId, "current-user")
        } catch (e: IllegalStateException) {
            Timber.e(e, "Error joining shopping list")
            trackInvocation(success = false, reason = "join_failed")
            return "Failed to join list: ${e.message}"
        }

        trackInvocation(success = true)
        "Joined shopping list '${shopping.title}' (ID: $shoppingId)"
    }.getOrElse { e ->
        Timber.e(e, "Error in joinShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to join shopping list: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "join_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("join_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
