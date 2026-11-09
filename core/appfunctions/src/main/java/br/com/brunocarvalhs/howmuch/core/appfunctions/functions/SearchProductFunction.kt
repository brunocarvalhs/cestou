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
 * Searches for products in a shopping list by name or category via AppFunctions.
 *
 * **Function Name:** `search_product`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `query`: Search term (name or category)
 *
 * **Returns:** List of matching products with details
 */
class SearchProductFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun searchProduct(shoppingId: String, query: String): String = runCatching {
        if (query.isBlank()) {
            trackInvocation(success = false, reason = "empty_query")
            return "Query cannot be empty"
        }

        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        val products = productsUseCase(shoppingId).first()
        val results = products.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)
        }

        if (results.isEmpty()) {
            trackInvocation(success = true)
            return "No products found matching '$query'"
        }

        val list = results.map { product ->
            "${product.name} (qty: ${product.quantity}, $${"%.2f".format(product.total)})"
        }.joinToString("\n")

        trackInvocation(success = true)
        "Found ${results.size} products:\n$list"
    }.getOrElse { e ->
        Timber.e(e, "Error in searchProduct")
        trackInvocation(success = false, reason = "exception")
        "Failed to search products: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "search_product",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("search_product failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
