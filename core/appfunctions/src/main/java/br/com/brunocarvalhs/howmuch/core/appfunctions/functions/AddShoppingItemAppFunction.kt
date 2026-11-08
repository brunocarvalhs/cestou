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
 * Adds a product to a shopping list via AppFunctions.
 *
 * **Function Name:** `add_shopping_item`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 * - `productName`: Name of the product to add
 * - `price`: Price per unit (optional, defaults to 0.0)
 * - `quantity`: Quantity to add (optional, defaults to 1.0)
 *
 * **Returns:** Success message or error description
 */
class AddShoppingItemAppFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun addShoppingItem(
        shoppingId: String,
        productName: String,
        price: Float = 0f,
        quantity: Float = 1f,
    ): String = runCatching {
        // Validate shopping list exists
        if (shoppingRepository.getById(shoppingId) == null) {
            val reason = "shopping_list_not_found"
            trackInvocation(success = false, reason = reason)
            return "Shopping list not found: $shoppingId"
        }

        // Create product
        val product = Product(
            id = System.currentTimeMillis().toString(),
            name = productName,
            quantity = quantity.toDouble(),
            price = price.toDouble(),
            isPurchased = false,
            category = "Outros"
        )

        // Save product
        productsUseCase.save(product, shoppingId).onFailure {
            Timber.e(it, "Error adding product to shopping list")
            trackInvocation(success = false, reason = "save_failed")
            return "Error adding product: ${it.message}"
        }

        trackInvocation(success = true)
        "Product '$productName' added successfully"
    }.getOrElse { e ->
        Timber.e(e, "Error in addShoppingItem")
        trackInvocation(success = false, reason = "exception")
        "Failed to add product: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "add_shopping_item",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("add_shopping_item failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
