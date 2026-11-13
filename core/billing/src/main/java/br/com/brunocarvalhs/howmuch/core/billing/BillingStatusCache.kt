package br.com.brunocarvalhs.howmuch.core.billing

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import timber.log.Timber

/**
 * Last status Play confirmed for one Cestou account, kept so a Pro user still sees Pro offline
 * on cold start (sdd-058). The entry records which account it belongs to: [load] answers only for
 * that account, so a status cached for one account never leaks to another on the same device.
 */
internal class BillingStatusCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Status cached for [accountId]; Free when the entry belongs to another account or is missing. */
    fun load(accountId: String): SubscriptionStatus {
        val name = prefs.getString(KEY_CACHED_STATUS, null)
        if (prefs.getString(KEY_OWNER_ACCOUNT_ID, null) != accountId || name == null) {
            return SubscriptionStatus.FREE
        }
        return try {
            SubscriptionStatus.valueOf(name)
        } catch (_: IllegalArgumentException) {
            SubscriptionStatus.FREE
        }
    }

    fun save(accountId: String, status: SubscriptionStatus) {
        try {
            prefs.edit()
                .putString(KEY_OWNER_ACCOUNT_ID, accountId)
                .putString(KEY_CACHED_STATUS, status.name)
                .remove(KEY_LEGACY_CACHED_STATUS)
                .apply()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to cache subscription status")
        }
    }

    companion object {
        private const val PREFS_NAME = "billing_preferences"
        private const val KEY_OWNER_ACCOUNT_ID = "cached_subscription_owner"
        private const val KEY_CACHED_STATUS = "cached_subscription_status_v2"

        /** Pre-sdd-058 entry, written without an owner; dropped on the next save. */
        private const val KEY_LEGACY_CACHED_STATUS = "cached_subscription_status"
        private const val TAG = "BillingStatusCache"
    }
}
