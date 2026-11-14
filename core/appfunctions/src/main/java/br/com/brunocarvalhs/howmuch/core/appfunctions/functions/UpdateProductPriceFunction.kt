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
 * Updates the price of a product in a shopping list via AppFunctions.
 *
 * **Function Name:** `update_product_price`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `productId`: ID of the product
 * - `newPrice`: New price value
 *
 * **Returns:** Confirmation with old and new prices
 */
class UpdateProductPriceFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun updateProductPrice(
        shoppingId: String,
        productId: String,
        newPrice: Float,
    ): String = runCatching {
        if (newPrice < 0) {
            trackInvocation(success = false, reason = "invalid_price")
            return "Price cannot be negative"
        }

        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        val products = productsUseCase(shoppingId).first()
        val product = products.find { it.id == productId }
            ?: return run {
                trackInvocation(success = false, reason = "product_not_found")
                "Product not found: $productId"
            }

        val oldPrice = product.price
        val updatedProduct = product.copy(price = newPrice.toDouble())
        productsUseCase.update(updatedProduct, shoppingId).onFailure {
            Timber.e(it, "Error updating product price")
            trackInvocation(success = false, reason = "update_failed")
            return "Error updating product: ${it.message}"
        }

        trackInvocation(success = true)
        "Product '${product.name}' price updated from $${"%.2f".format(oldPrice)} to $${"%.2f".format(newPrice)}"
    }.getOrElse { e ->
        Timber.e(e, "Error in updateProductPrice")
        trackInvocation(success = false, reason = "exception")
        "Failed to update price: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "update_product_price",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("update_product_price failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
