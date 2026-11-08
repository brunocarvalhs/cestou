package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates a new shopping list via AppFunctions.
 *
 * **Function Name:** `create_shopping_list`
 *
 * **Parameters:**
 * - `title`: Name of the shopping list
 * - `budget`: Initial budget (optional, defaults to null)
 *
 * **Returns:** ID of the created shopping list or error description
 */
class CreateShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun createShoppingList(
        title: String,
        budget: Float? = null,
    ): String = runCatching {
        // Validate input
        if (title.isBlank()) {
            trackInvocation(success = false, reason = "blank_title")
            return "Title cannot be empty"
        }

        // Create shopping list
        val listId = System.currentTimeMillis().toString()
        val shopping = Shopping(
            id = listId,
            title = title,
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap(),
            budget = budget?.toDouble(),
            emoji = Shopping.DEFAULT_EMOJI
        )

        shoppingRepository.create(shopping)
        trackInvocation(success = true)
        listId
    }.getOrElse { e ->
        Timber.e(e, "Error in createShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to create shopping list: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "create_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("create_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
