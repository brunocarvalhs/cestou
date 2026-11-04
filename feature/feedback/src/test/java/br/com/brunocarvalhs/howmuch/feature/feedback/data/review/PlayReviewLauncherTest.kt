package br.com.brunocarvalhs.howmuch.feature.feedback.data.review

import android.app.Activity
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReviewLauncherTest {

    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val activity = mockk<Activity>()

    @Test
    fun `returns true when the flow completes`() = runTest {
        val launcher = PlayReviewLauncher({ }, crashReporter, timeoutMs = 1_000)

        assertTrue(launcher.launch(activity))
    }

    @Test
    fun `a failure such as missing Play Services is reported and never thrown`() = runTest {
        val failure = IllegalStateException("no play services")
        val launcher = PlayReviewLauncher({ throw failure }, crashReporter, timeoutMs = 1_000)

        assertFalse(launcher.launch(activity))
        // A recuperação de stack trace das coroutines pode recriar a exceção: compara tipo e mensagem.
        verify {
            crashReporter.recordException(
                match { it is IllegalStateException && it.message == failure.message },
                any()
            )
        }
    }

    @Test
    fun `a timeout is reported to the crash reporter`() = runTest {
        val launcher = PlayReviewLauncher({ delay(10_000) }, crashReporter, timeoutMs = 100)

        assertFalse(launcher.launch(activity))
        verify { crashReporter.recordException(any()) }
    }
}
