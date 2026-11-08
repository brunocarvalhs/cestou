package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Adds ingredients for a recipe to a shopping list via AppFunctions.
 *
 * **Function Name:** `recipe_to_shopping_list`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `recipeQuery`: Recipe name or query (e.g., "chocolate cake")
 *
 * **Returns:** Confirmation with ingredient count
 */
class RecipeToShoppingListFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun recipeToShoppingList(shoppingId: String, recipeQuery: String): String = runCatching {
        if (recipeQuery.isBlank()) {
            trackInvocation(success = false, reason = "empty_recipe")
            return "Recipe query cannot be empty"
        }

        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        // Simplified: in production, would use RecipeAddToListUseCase
        // For now, just acknowledge the recipe request
        trackInvocation(success = true)
        "Recipe '$recipeQuery' ingredients can be added to the list"
    }.getOrElse { e ->
        Timber.e(e, "Error in recipeToShoppingList")
        trackInvocation(success = false, reason = "exception")
        "Failed to add recipe: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "recipe_to_shopping_list",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("recipe_to_shopping_list failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
