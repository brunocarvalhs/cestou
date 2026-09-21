package br.com.brunocarvalhs.howmuch.core.analytics.exception

import br.com.brunocarvalhs.howmuch.core.common.exception.AppException

class AnalyticsException(
    message: String? = null,
    cause: Throwable? = null
) : AppException(tag = TAG, message = message, cause = cause) {

    companion object {
        const val TAG = "analytics"
    }
}
