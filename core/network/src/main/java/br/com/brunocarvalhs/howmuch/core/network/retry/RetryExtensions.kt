package br.com.brunocarvalhs.howmuch.core.network.retry

import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Execute a suspend function with exponential backoff retry logic.
 *
 * @param retryPolicy Policy determining retryability and backoff timing
 * @param block The suspend function to execute
 * @return Result of block on success
 * @throws Exception if all retries exhausted
 */
internal suspend inline fun <T> withRetry(
    retryPolicy: RetryPolicy,
    crossinline block: suspend () -> T
): T {
    var lastException: IOException? = null
    var attempt = 0

    while (attempt < retryPolicy.getMaxAttempts()) {
        attempt++
        try {
            return block()
        } catch (e: IOException) {
            if (!retryPolicy.isRetryable(e)) throw e
            lastException = e
            if (attempt < retryPolicy.getMaxAttempts()) {
                delay(retryPolicy.getBackoffDelayMs(attempt))
            }
        }
    }

    throw lastException ?: IOException("Max retries exhausted")
}
