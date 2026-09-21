package br.com.brunocarvalhs.howmuch.core.common.contract

/**
 * Abstrai o backend de crash reporting para que a implementação (hoje Firebase Crashlytics)
 * possa ser trocada sem alterar quem reporta exceções.
 */
interface CrashReporter {

    fun recordException(throwable: Throwable, extras: Map<String, String> = emptyMap())

    fun log(message: String)

    fun setUserId(id: String?)
}
