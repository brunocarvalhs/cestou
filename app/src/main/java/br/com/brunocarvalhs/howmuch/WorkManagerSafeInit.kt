package br.com.brunocarvalhs.howmuch

import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import timber.log.Timber

/**
 * Runs WorkManager's manual initialization defensively. Some devices report `SDK_INT >= 34`
 * without actually implementing `JobScheduler.forNamespace`, which WorkManager's
 * `SystemJobScheduler` calls unconditionally and crashes the whole app at process start with
 * `NoSuchMethodError` (Crashlytics fatal seen on a Pixel 8 Pro). [initialize] is the real
 * `WorkManager.initialize(...)` call, injected so this can be unit tested without Robolectric.
 */
internal fun initializeWorkManagerSafely(crashReporter: CrashReporter, initialize: () -> Unit) {
    try {
        initialize()
    } catch (t: Throwable) {
        Timber.e(t, "WorkManager initialization failed; shopping reminders will be unavailable")
        crashReporter.recordException(t)
    }
}
