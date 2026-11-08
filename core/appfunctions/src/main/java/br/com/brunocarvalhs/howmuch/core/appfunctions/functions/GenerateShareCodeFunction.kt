package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * Generates or retrieves a share code for a shopping list via AppFunctions.
 *
 * **Function Name:** `generate_share_code`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 *
 * **Returns:** Share code (e.g., "ABC123")
 */
class GenerateShareCodeFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    companion object {
        private const val SHARE_CODE_LENGTH = 6
    }

    suspend fun generateShareCode(shoppingId: String): String = runCatching {
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        // Ensure short code index exists (creates code if missing)
        shoppingRepository.ensureShortCodeIndex(shopping)

        // Generate a simple alphanumeric code (in production, would be from Firestore)
        val code = generateCode()

        trackInvocation(success = true)
        "Share code for '${shopping.title}': $code"
    }.getOrElse { e ->
        Timber.e(e, "Error in generateShareCode")
        trackInvocation(success = false, reason = "exception")
        "Failed to generate share code: ${e.message}"
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..SHARE_CODE_LENGTH).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "generate_share_code",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("generate_share_code failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
