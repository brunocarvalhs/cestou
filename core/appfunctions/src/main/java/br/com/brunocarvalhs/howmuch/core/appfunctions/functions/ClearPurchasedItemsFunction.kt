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
 * Removes all purchased items from a shopping list via AppFunctions.
 *
 * **Function Name:** `clear_purchased_items`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 *
 * **Returns:** Count of items removed
 */
class ClearPurchasedItemsFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun clearPurchasedItems(shoppingId: String): String = runCatching {
        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        val products = productsUseCase(shoppingId).first()
        val purchasedItems = products.filter { it.isPurchased }
        var removedCount = 0

        for (product in purchasedItems) {
            productsUseCase.delete(product.id, shoppingId).onSuccess {
                removedCount++
            }.onFailure {
                Timber.e(it, "Error removing product ${product.id}")
            }
        }

        trackInvocation(success = true)
        "Removed $removedCount purchased items"
    }.getOrElse { e ->
        Timber.e(e, "Error in clearPurchasedItems")
        trackInvocation(success = false, reason = "exception")
        "Failed to clear purchased items: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "clear_purchased_items",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("clear_purchased_items failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
