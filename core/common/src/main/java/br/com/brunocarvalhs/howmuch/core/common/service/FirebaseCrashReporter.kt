package br.com.brunocarvalhs.howmuch.core.common.service

import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.common.exception.AppException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class FirebaseCrashReporter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) : CrashReporter {

    override fun recordException(throwable: Throwable, extras: Map<String, String>) {
        if (throwable is AppException) {
            crashlytics.setCustomKey(KEY_TAG, throwable.tag)
        }
        extras.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setUserId(id: String?) {
        crashlytics.setUserId(id.orEmpty())
    }

    companion object {
        private const val KEY_TAG = "app_exception_tag"
    }
}
