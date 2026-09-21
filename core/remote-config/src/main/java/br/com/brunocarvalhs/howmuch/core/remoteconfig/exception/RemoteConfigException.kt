package br.com.brunocarvalhs.howmuch.core.remoteconfig.exception

import br.com.brunocarvalhs.howmuch.core.common.exception.AppException

class RemoteConfigException(
    message: String? = null,
    cause: Throwable? = null
) : AppException(tag = TAG, message = message, cause = cause) {

    companion object {
        const val TAG = "remote_config"
    }
}
