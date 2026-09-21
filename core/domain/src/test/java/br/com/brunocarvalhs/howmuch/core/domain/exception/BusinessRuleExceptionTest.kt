package br.com.brunocarvalhs.howmuch.core.domain.exception

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BusinessRuleExceptionTest {

    @Test
    fun `exposes the tag, message and cause it was built with`() {
        val cause = IllegalStateException("root cause")

        val exception = BusinessRuleException(tag = "shopping_ownership", message = "not allowed", cause = cause)

        assertEquals("shopping_ownership", exception.tag)
        assertEquals("not allowed", exception.message)
        assertSame(cause, exception.cause)
    }
}
