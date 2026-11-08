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
 * Gets total spending for the current month via AppFunctions.
 *
 * **Function Name:** `get_monthly_spending`
 *
 * **Parameters:**
 * - `month`: Optional month in format "YYYY-MM"
 *
 * **Returns:** Total spent for the month
 */
class GetMonthlySpendingFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun getMonthlySpending(month: String? = null): String = runCatching {
        val lists = shoppingRepository.getAll()
        var totalSpent = 0.0

        for (shopping in lists) {
            val products = productsUseCase(shopping.id).first()
            totalSpent += products.sumOf { it.total }
        }

        trackInvocation(success = true)
        val monthLabel = month ?: "Current month"
        "Monthly spending ($monthLabel): $${"%.2f".format(totalSpent)} across ${lists.size} lists"
    }.getOrElse { e ->
        Timber.e(e, "Error in getMonthlySpending")
        trackInvocation(success = false, reason = "exception")
        "Failed to get monthly spending: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "get_monthly_spending",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("get_monthly_spending failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
