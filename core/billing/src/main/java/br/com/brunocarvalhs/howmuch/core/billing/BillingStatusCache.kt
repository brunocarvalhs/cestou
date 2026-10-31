package br.com.brunocarvalhs.howmuch.core.billing

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import timber.log.Timber

internal class BillingStatusCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): SubscriptionStatus {
        val name = prefs.getString(KEY_CACHED_STATUS, null) ?: return SubscriptionStatus.FREE
        return try {
            SubscriptionStatus.valueOf(name)
        } catch (_: IllegalArgumentException) {
            SubscriptionStatus.FREE
        }
    }

    fun save(status: SubscriptionStatus) {
        try {
            prefs.edit().putString(KEY_CACHED_STATUS, status.name).apply()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to cache subscription status")
        }
    }

    companion object {
        private const val PREFS_NAME = "billing_preferences"
        private const val KEY_CACHED_STATUS = "cached_subscription_status"
        private const val TAG = "BillingStatusCache"
    }
}
