package br.com.brunocarvalhs.howmuch

import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkManagerSafeInitTest {

    private val crashReporter = mockk<CrashReporter>(relaxed = true)

    @Test
    fun `initializeWorkManagerSafely runs initialize and never reports when it succeeds`() {
        var invoked = false

        initializeWorkManagerSafely(crashReporter) { invoked = true }

        assertTrue(invoked)
        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
    }

    @Test
    fun `initializeWorkManagerSafely catches NoSuchMethodError from initialize and reports it`() {
        val error = NoSuchMethodError("forNamespace")

        initializeWorkManagerSafely(crashReporter) { throw error }

        verify(exactly = 1) { crashReporter.recordException(error, any()) }
    }

    @Test
    fun `initializeWorkManagerSafely catches any Throwable from initialize without propagating`() {
        val error = IllegalStateException("unexpected framework error")

        initializeWorkManagerSafely(crashReporter) { throw error }

        verify(exactly = 1) { crashReporter.recordException(error, any()) }
    }
}
