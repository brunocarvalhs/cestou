package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

/**
 * Lists all members of a shopping list with their roles via AppFunctions.
 *
 * **Function Name:** `list_list_members`
 *
 * **Parameters:**
 * - `shoppingId`: ID of the shopping list
 *
 * **Returns:** Member list with roles
 */
class ListListMembersFunction @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val crashlytics: FirebaseCrashlytics,
) {

    suspend fun listListMembers(shoppingId: String): String = runCatching {
        val shopping = shoppingRepository.getById(shoppingId)
            ?: return run {
                trackInvocation(success = false, reason = "shopping_list_not_found")
                "Shopping list not found: $shoppingId"
            }

        if (shopping.users.isEmpty()) {
            trackInvocation(success = true)
            return "No members in this list"
        }

        val members = shopping.users.map { userId ->
            val role = shopping.roles[userId] ?: "viewer"
            "• $userId ($role)"
        }.joinToString("\n")

        trackInvocation(success = true)
        "${shopping.users.size} members:\n$members"
    }.getOrElse { e ->
        Timber.e(e, "Error in listListMembers")
        trackInvocation(success = false, reason = "exception")
        "Failed to list members: ${e.message}"
    }

    private fun trackInvocation(success: Boolean, reason: String? = null) {
        val params = mutableMapOf<String, Any?>(
            AnalyticsParams.ACTION_ID to "list_list_members",
            AnalyticsParams.SUCCESS to success
        )
        if (reason != null) {
            params[AnalyticsParams.REASON] to reason
            crashlytics.log("list_list_members failed: $reason")
        }
        analyticsTracker.trackEvent("app_function_invoked", params)
    }
}
