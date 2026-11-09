package br.com.brunocarvalhs.howmuch.core.network.retry

/**
 * Policy for retrying failed requests with exponential backoff.
 *
 * Retryable errors: network timeouts, connection errors, 5xx, 429 (rate limit).
 * Non-retryable: 4xx (except 429), invalid API key, parse errors.
 */
internal interface RetryPolicy {

    /**
     * Determine if an error is retryable.
     *
     * @param error The exception that occurred
     * @param statusCode HTTP status code (null if non-HTTP error)
     * @return true if the request should be retried
     */
    fun isRetryable(error: Throwable, statusCode: Int? = null): Boolean

    /**
     * Calculate backoff delay in milliseconds for given attempt.
     *
     * Formula: initialDelayMs × (multiplier ^ (attempt - 1))
     * Example: 1000 × (1.5 ^ 0) = 1000ms, 1000 × (1.5 ^ 1) = 1500ms, etc.
     *
     * @param attempt Attempt number (1-indexed)
     * @return Delay in milliseconds
     */
    fun getBackoffDelayMs(attempt: Int): Long

    /**
     * Maximum number of retry attempts (from remote config).
     */
    fun getMaxAttempts(): Int
}
