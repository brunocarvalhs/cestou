package br.com.brunocarvalhs.howmuch.core.network.retry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ExponentialBackoffRetryPolicyTest {

    private val policy = ExponentialBackoffRetryPolicy(
        maxAttempts = 3,
        initialDelayMs = 1000,
        multiplier = 1.5
    )

    @Test
    fun `timeout message is retryable`() {
        val error = IOException("Socket timeout")
        assertTrue(policy.isRetryable(error))
    }

    @Test
    fun `connection error is retryable`() {
        val error = IOException("Connection refused")
        assertTrue(policy.isRetryable(error))
    }

    @Test
    fun `connection timeout message is retryable`() {
        val error = IOException("Connection timeout")
        assertTrue(policy.isRetryable(error))
    }

    @Test
    fun `HTTP 500 is retryable`() {
        assertTrue(policy.isRetryable(IOException("Server error"), 500))
    }

    @Test
    fun `HTTP 429 rate limit is retryable`() {
        assertTrue(policy.isRetryable(IOException("Rate limited"), 429))
    }

    @Test
    fun `HTTP 401 unauthorized is NOT retryable`() {
        assertFalse(policy.isRetryable(IOException("Unauthorized"), 401))
    }

    @Test
    fun `HTTP 404 not found is NOT retryable`() {
        assertFalse(policy.isRetryable(IOException("Not found"), 404))
    }

    @Test
    fun `generic exception is NOT retryable`() {
        val error = IllegalArgumentException("Invalid input")
        assertFalse(policy.isRetryable(error))
    }

    @Test
    fun `backoff delay follows exponential formula`() {
        // Attempt 1: 1000 * 1.5^0 = 1000ms
        assertEquals(1000, policy.getBackoffDelayMs(1))

        // Attempt 2: 1000 * 1.5^1 = 1500ms
        assertEquals(1500, policy.getBackoffDelayMs(2))

        // Attempt 3: 1000 * 1.5^2 = 2250ms
        assertEquals(2250, policy.getBackoffDelayMs(3))
    }

    @Test
    fun `max attempts from config`() {
        assertEquals(3, policy.getMaxAttempts())
    }

    private fun assertEquals(expected: Long, actual: Long) {
        if (expected != actual) {
            throw AssertionError("Expected $expected, got $actual")
        }
    }

    private fun assertEquals(expected: Int, actual: Int) {
        if (expected != actual) {
            throw AssertionError("Expected $expected, got $actual")
        }
    }
}
