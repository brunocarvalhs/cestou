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
 * Gets all products in a shopping list with their status via AppFunctions.
 *
 * **Function Name:** `get_shopping_products`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 *
 * **Returns:** Text listing of products with status
 */
class GetShoppingProductsFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun getShoppingProducts(shoppingId: String): String = runCatching {
        // Get shopping list
        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        // Get products
        val products = productsUseCase(shoppingId).first()

        if (products.isEmpty()) {
            trackInvocation(success = true)
            return "No products in this list"
        }

        val productsList = products.map { product ->
            val status = if (product.isPurchased) "✓" else "○"
            "$status ${product.name} (qty: ${product.quantity}, $${"%.2f".format(product.total)})"
        }.joinToString("\n")

        trackInvocation(success = true)
        "Products in ${products.size} items:\n$productsList"
    }.getOrElse { e ->
        Timber.e(e, "Error in getShoppingProducts")
        trackInvocation(success = false, reason = "exception")
        "Failed to get products: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "get_shopping_products",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("get_shopping_products failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
