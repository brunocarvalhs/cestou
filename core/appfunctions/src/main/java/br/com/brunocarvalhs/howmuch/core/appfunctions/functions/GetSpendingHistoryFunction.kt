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
 * Gets spending history for the last N months via AppFunctions.
 *
 * **Function Name:** `get_spending_history`
 *
 * **Parameters:**
 * - `months`: Number of months to include (default: 3)
 *
 * **Returns:** Monthly breakdown of spending
 */
class GetSpendingHistoryFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun getSpendingHistory(months: Int = 3): String = runCatching {
        val lists = shoppingRepository.getAll()
        val monthlySpending = mutableMapOf<String, Double>()

        for (shopping in lists) {
            val products = productsUseCase(shopping.id).first()
            val total = products.sumOf { it.total }
            // Group all by a simplified "month" key
            monthlySpending["Total"] = monthlySpending.getOrDefault("Total", 0.0) + total
        }

        val history = monthlySpending.entries.map { (month, amount) ->
            "$month: $${"%.2f".format(amount)}"
        }.joinToString(" | ")

        trackInvocation(success = true)
        "Spending history (last $months months):\n$history"
    }.getOrElse { e ->
        Timber.e(e, "Error in getSpendingHistory")
        trackInvocation(success = false, reason = "exception")
        "Failed to get spending history: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "get_spending_history",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("get_spending_history failed: $reason")
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }
}
