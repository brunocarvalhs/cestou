package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Removes a product from a shopping list via AppFunctions.
 *
 * **Function Name:** `remove_product`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `productId`: ID of the product to remove
 *
 * **Returns:** Status message or error description
 */
class RemoveProductFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun removeProduct(
        shoppingId: String,
        productId: String,
    ): String = runCatching {
        // Validate shopping list exists
        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        // Delete product
        productsUseCase.delete(productId, shoppingId).onFailure {
            Timber.e(it, "Error removing product")
            trackInvocation(success = false, reason = "delete_failed")
            return "Error removing product: ${it.message}"
        }

        trackInvocation(success = true)
        "Product removed successfully"
    }.getOrElse { e ->
        Timber.e(e, "Error in removeProduct")
        trackInvocation(success = false, reason = "exception")
        "Failed to remove product: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "remove_product",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("remove_product failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
