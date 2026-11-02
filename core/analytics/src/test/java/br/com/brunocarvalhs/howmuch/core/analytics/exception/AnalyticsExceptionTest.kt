package br.com.brunocarvalhs.howmuch.core.analytics.exception

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsExceptionTest {

    @Test
    fun `analytics exception properties`() {
        val cause = RuntimeException("root cause")
        val exception = AnalyticsException("failed", cause)

        assertEquals("analytics", exception.tag)
        assertEquals("failed", exception.message)
        assertEquals(cause, exception.cause)
    }
}
