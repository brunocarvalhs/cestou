package br.com.brunocarvalhs.howmuch.push

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.PushPayload
import br.com.brunocarvalhs.howmuch.core.domain.model.PushSaveOutcome
import com.google.firebase.messaging.RemoteMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instantiated directly: Hilt's generated base class injects in `onCreate`, which this test skips by
 * assigning the handler itself.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class CestouMessagingServiceTest {

    private val handler = mockk<PushNotificationHandler>()
    private val service = CestouMessagingService().also { it.pushNotificationHandler = handler }

    private fun message() = RemoteMessage.Builder("to")
        .setMessageId("m1")
        .addData("title", "T")
        .addData("message", "M")
        .build()

    @Test
    fun `a message received in the foreground is handed to the handler`() {
        var seen: PushPayload? = null
        coEvery { handler.handle(any(), PushSource.MESSAGE) } coAnswers {
            seen = firstArg()
            PushSaveOutcome.Saved
        }

        service.onMessageReceived(message())

        assertEquals("m1", seen?.messageId)
        assertEquals(mapOf("title" to "T", "message" to "M"), seen?.data)
        coVerify(exactly = 1) { handler.handle(any(), PushSource.MESSAGE) }
    }

    @Test
    fun `a handler that never finishes does not hold the service forever`() {
        coEvery { handler.handle(any(), any()) } coAnswers {
            delay(Long.MAX_VALUE)
            PushSaveOutcome.Saved
        }
        val started = System.nanoTime()

        service.onMessageReceived(message())

        val elapsedSeconds = (System.nanoTime() - started) / 1_000_000_000
        assertEquals(true, elapsedSeconds in 9..15)
    }
}
