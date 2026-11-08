package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Adds multiple products to a shopping list at once via AppFunctions.
 *
 * **Function Name:** `add_products_multiple`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `productsJson`: JSON array of {name, quantity, price, category}
 *
 * **Returns:** Confirmation with count of added products
 */
class AddProductsMultipleFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    companion object {
        private const val MIN_PRODUCT_FIELDS = 2
        private const val INDEX_NAME = 0
        private const val INDEX_QUANTITY = 1
        private const val INDEX_PRICE = 2
        private const val INDEX_CATEGORY = 3
    }

    suspend fun addProductsMultiple(shoppingId: String, productsJson: String): String = runCatching {
        if (productsJson.isBlank()) {
            trackInvocation(success = false, reason = "empty_products")
            return "Products list cannot be empty"
        }

        if (shoppingRepository.getById(shoppingId) == null) {
            trackInvocation(success = false, reason = "shopping_list_not_found")
            return "Shopping list not found: $shoppingId"
        }

        // Parse products (simplified: "name,qty,price,category;name,qty,price,category")
        val productLines = productsJson.split(";")
        var addedCount = 0

        for (line in productLines) {
            val parts = line.trim().split(",")
            if (parts.size >= MIN_PRODUCT_FIELDS) {
                val name = parts.getOrNull(INDEX_NAME)?.trim()
                if (name != null) {
                    val quantity = parts.getOrNull(INDEX_QUANTITY)?.trim()?.toDoubleOrNull() ?: 1.0
                    val price = parts.getOrNull(INDEX_PRICE)?.trim()?.toDoubleOrNull() ?: 0.0
                    val category = parts.getOrNull(INDEX_CATEGORY)?.trim() ?: "Other"

                    val product = Product(
                        id = "temp-${System.currentTimeMillis()}-${name.hashCode()}",
                        name = name,
                        quantity = quantity,
                        price = price,
                        category = category
                    )

                    productsUseCase.save(product, shoppingId).onSuccess {
                        addedCount++
                    }.onFailure {
                        Timber.e(it, "Error adding product $name")
                    }
                }
            }
        }

        trackInvocation(success = true)
        "Added $addedCount products successfully"
    }.getOrElse { e ->
        Timber.e(e, "Error in addProductsMultiple")
        trackInvocation(success = false, reason = "exception")
        "Failed to add products: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "add_products_multiple",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("add_products_multiple failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
