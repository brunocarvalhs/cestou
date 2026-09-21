package br.com.brunocarvalhs.howmuch.core.common.service

import br.com.brunocarvalhs.howmuch.core.common.exception.AppException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

class FirebaseCrashReporterTest {

    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val reporter = FirebaseCrashReporter(crashlytics)

    @Test
    fun `recordException sets tag custom key for AppException before recording`() {
        val exception = AppException(tag = "shopping_ownership", message = "boom")

        reporter.recordException(exception)

        verifyOrder {
            crashlytics.setCustomKey("app_exception_tag", "shopping_ownership")
            crashlytics.recordException(exception)
        }
    }

    @Test
    fun `recordException does not set tag custom key for plain exceptions`() {
        val exception = IllegalStateException("boom")

        reporter.recordException(exception)

        verify(exactly = 0) { crashlytics.setCustomKey("app_exception_tag", any<String>()) }
        verify { crashlytics.recordException(exception) }
    }

    @Test
    fun `recordException forwards every extra as a custom key`() {
        val exception = IllegalStateException("boom")

        reporter.recordException(exception, mapOf("shopping_id" to "list-1", "action" to "delete"))

        verify { crashlytics.setCustomKey("shopping_id", "list-1") }
        verify { crashlytics.setCustomKey("action", "delete") }
    }

    @Test
    fun `log forwards message to crashlytics`() {
        reporter.log("hello")

        verify { crashlytics.log("hello") }
    }

    @Test
    fun `setUserId forwards non-null id`() {
        reporter.setUserId("user-1")

        verify { crashlytics.setUserId("user-1") }
    }

    @Test
    fun `setUserId forwards empty string when id is null`() {
        reporter.setUserId(null)

        every { crashlytics.setUserId(any()) }
        verify { crashlytics.setUserId("") }
    }
}
