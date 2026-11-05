package br.com.brunocarvalhs.howmuch.core.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle

internal class BillingLifecycleObserver(
    private val throttleMs: Long = DEFAULT_THROTTLE_MS,
    private val onResume: () -> Unit
) : Application.ActivityLifecycleCallbacks {

    private var lastResumeTimestamp = 0L

    override fun onActivityResumed(activity: Activity) {
        val now = System.currentTimeMillis()
        if (now - lastResumeTimestamp >= throttleMs) {
            lastResumeTimestamp = now
            onResume()
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    companion object {
        const val DEFAULT_THROTTLE_MS = 3_000L
    }
}
