package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Marks a product as purchased or unpurchased via AppFunctions.
 *
 * **Function Name:** `mark_product_purchased`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `productId`: ID of the product to mark
 * - `isPurchased`: true to mark as purchased, false to unmark
 *
 * **Returns:** Status message with product name and new state
 */
class MarkProductPurchasedFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun markProductPurchased(
        shoppingId: String,
        productId: String,
        isPurchased: Boolean,
    ): String = runCatching {
        // Validate shopping list exists
        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        // Get product and update
        val products = productsUseCase(shoppingId).first()
        val product = products.find { it.id == productId }
            ?: return run {
                trackInvocation(success = false, reason = "product_not_found")
                "Product not found: $productId"
            }

        val updatedProduct = product.copy(isPurchased = isPurchased)
        productsUseCase.update(updatedProduct, shoppingId).onFailure {
            Timber.e(it, "Error updating product purchase status")
            trackInvocation(success = false, reason = "update_failed")
            return "Error updating product: ${it.message}"
        }

        trackInvocation(success = true)
        val status = if (isPurchased) "purchased" else "unpurchased"
        "Product '${product.name}' marked as $status"
    }.getOrElse { e ->
        Timber.e(e, "Error in markProductPurchased")
        trackInvocation(success = false, reason = "exception")
        "Failed to update product: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "mark_product_purchased",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("mark_product_purchased failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
