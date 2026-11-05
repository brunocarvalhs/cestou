package br.com.brunocarvalhs.howmuch.core.analytics.util

private const val MAX_EVENT_NAME = 40
private const val MAX_PARAM_NAME = 40
private const val MAX_PARAM_VALUE = 100
private const val MAX_PARAMS = 25
private val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
private val VALID_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*$")

/**
 * O Firebase descarta em silêncio eventos e parâmetros fora dos limites dele; validar aqui
 * transforma esse descarte invisível em ajuste determinístico (e testável).
 */
internal object AnalyticsSanitizer {

    /** `null` quando [name] nunca seria aceito pelo Firebase. */
    fun eventName(name: String): String? = name.takeIf(::isValidName)?.take(MAX_EVENT_NAME)

    fun params(params: Map<String, Any?>): Map<String, Any?> = params.entries
        .asSequence()
        .filter { (key, value) -> value != null && isValidName(key) }
        .take(MAX_PARAMS)
        .associate { (key, value) ->
            key.take(MAX_PARAM_NAME) to if (value is String) value.take(MAX_PARAM_VALUE) else value
        }

    private fun isValidName(name: String): Boolean =
        VALID_NAME.matches(name) && RESERVED_PREFIXES.none { name.startsWith(it) }
}
