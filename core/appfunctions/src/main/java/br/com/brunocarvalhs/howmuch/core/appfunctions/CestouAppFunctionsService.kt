package br.com.brunocarvalhs.howmuch.core.appfunctions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.appfunctions.functions.AddShoppingItemAppFunction
import br.com.brunocarvalhs.howmuch.core.appfunctions.functions.CreateShoppingListFunction
import br.com.brunocarvalhs.howmuch.core.appfunctions.functions.GetShoppingListSummaryFunction
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppFunctions handler that exposes Cestou shopping list operations to system assistants
 * (e.g., Gemini on Android 16+).
 *
 * This class provides the entry point for AppFunction invocations when the user
 * asks their assistant to perform actions on Cestou, such as "add milk to my shopping list"
 * or "show me my shopping list summary".
 *
 * Requires Android 16+ (API level 36+).
 *
 * **Exposed Functions:**
 * - `add_shopping_item`: Adds a product to a shopping list
 * - `get_shopping_list_summary`: Retrieves a shopping list summary
 * - `create_shopping_list`: Creates a new shopping list
 */
@Singleton
class CestouAppFunctionsService @Inject constructor(
    val addShoppingItemFunction: AddShoppingItemAppFunction,
    val getShoppingListSummaryFunction: GetShoppingListSummaryFunction,
    val createShoppingListFunction: CreateShoppingListFunction,
    private val featureFlagService: FeatureFlagService,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    init {
        Timber.d("CestouAppFunctionsService initialized")
    }

    /**
     * Checks if AppFunctions are enabled via feature flag.
     */
    suspend fun isEnabled(): Boolean {
        return featureFlagService.isEnabled(FeatureFlagKeys.APP_FUNCTIONS_ENABLED)
    }

    /**
     * Tracks an AppFunction invocation.
     */
    fun trackFunctionInvoked(functionName: String, success: Boolean, errorReason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to functionName,
            AnalyticsParams.SUCCESS to success
        )
        if (errorReason != null) {
            params[AnalyticsParams.REASON] = errorReason
        }
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FUNCTION_INVOKED, params)
    }

    /**
     * Records an AppFunction error to Crashlytics for debugging.
     */
    fun recordError(functionName: String, exception: Throwable, context: String = "") {
        val message = "AppFunction error in $functionName: $context"
        crashlytics.log(message)
        crashlytics.recordException(exception)
    }
}
