package br.com.brunocarvalhs.howmuch.core.network.retry

import kotlin.math.pow // Used in getBackoffDelayMs

/**
 * Exponential backoff retry policy with configurable parameters.
 *
 * Retryable errors: network timeouts, connection errors, 5xx status codes, 429 (rate limit).
 * Non-retryable: 4xx (except 429), invalid API key, parse errors.
 */
internal class ExponentialBackoffRetryPolicy(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    private val multiplier: Double = DEFAULT_MULTIPLIER
) : RetryPolicy {

    companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val DEFAULT_INITIAL_DELAY_MS = 1000L
        private const val DEFAULT_MULTIPLIER = 1.5
        private const val HTTP_SERVER_ERROR_MIN = 500
        private const val HTTP_RATE_LIMIT = 429
    }

    override fun isRetryable(error: Throwable, statusCode: Int?): Boolean {
        val message = error.message ?: ""
        return when {
            message.contains("timeout", ignoreCase = true) -> true
            message.contains("connection", ignoreCase = true) -> true
            statusCode != null && statusCode >= HTTP_SERVER_ERROR_MIN -> true
            statusCode == HTTP_RATE_LIMIT -> true
            else -> false
        }
    }

    override fun getBackoffDelayMs(attempt: Int): Long {
        val delayMs = initialDelayMs * multiplier.pow(attempt - 1)
        return delayMs.toLong()
    }

    override fun getMaxAttempts(): Int = maxAttempts
}
