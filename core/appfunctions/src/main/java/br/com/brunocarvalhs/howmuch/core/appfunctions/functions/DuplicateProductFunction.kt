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
 * Duplicates a product in a shopping list with a specified quantity via AppFunctions.
 *
 * **Function Name:** `duplicate_product`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `productId`: ID of the product to duplicate
 * - `quantity`: Quantity for the new product (default: 1.0)
 *
 * **Returns:** Confirmation with product name and new quantity
 */
class DuplicateProductFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun duplicateProduct(
        shoppingId: String,
        productId: String,
        quantity: Double = 1.0,
    ): String = runCatching {
        if (quantity <= 0) {
            trackInvocation(success = false, reason = "invalid_quantity")
            return "Quantity must be greater than 0"
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

        val newProduct = product.copy(
            id = "dup-${System.currentTimeMillis()}-${productId.hashCode()}",
            quantity = quantity
        )
        productsUseCase.save(newProduct, shoppingId).onFailure {
            Timber.e(it, "Error duplicating product")
            trackInvocation(success = false, reason = "save_failed")
            return "Error duplicating product: ${it.message}"
        }

        trackInvocation(success = true)
        "Product '${product.name}' duplicated with quantity $quantity"
    }.getOrElse { e ->
        Timber.e(e, "Error in duplicateProduct")
        trackInvocation(success = false, reason = "exception")
        "Failed to duplicate product: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "duplicate_product",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("duplicate_product failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
