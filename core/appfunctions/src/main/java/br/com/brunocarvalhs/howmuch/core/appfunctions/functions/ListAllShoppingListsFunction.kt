package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Lists all shopping lists accessible to the current user via AppFunctions.
 *
 * **Function Name:** `list_all_shopping_lists`
 *
 * **Returns:** Summary of all lists with item count and total spent
 */
class ListAllShoppingListsFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun listAllShoppingLists(): String = runCatching {
        val lists = shoppingRepository.getAll()

        if (lists.isEmpty()) {
            trackInvocation(success = true)
            return "No shopping lists found"
        }

        val summary = lists.map { shopping ->
            "${shopping.title} (Budget: $${"%.2f".format(shopping.budget)})"
        }.joinToString("\n")

        trackInvocation(success = true)
        "Your ${lists.size} lists:\n$summary"
    }.getOrElse { e ->
        Timber.e(e, "Error in listAllShoppingLists")
        trackInvocation(success = false, reason = "exception")
        "Failed to list shopping lists: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "list_all_shopping_lists",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("list_all_shopping_lists failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
