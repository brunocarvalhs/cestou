package br.com.brunocarvalhs.howmuch.core.common.exception

/**
 * Base para exceções da aplicação que devem ser facilmente identificáveis no Crashlytics.
 *
 * [tag] agrupa ocorrências relacionadas (ex: "ai_provider", "remote_config") e é reportado
 * como custom key pelo [br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter],
 * permitindo filtrar o Crashlytics por origem sem depender do nome da classe/stacktrace.
 */
open class AppException(
    val tag: String,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
