package br.com.brunocarvalhs.howmuch.push

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class PushPayloadExtractorTest {

    private fun trayIntent(vararg extras: Pair<String, Any>) = Intent().apply {
        putExtras(
            Bundle().apply {
                extras.forEach { (key, value) ->
                    when (value) {
                        is Long -> putLong(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
        )
    }

    @Test
    fun `a foreground message keeps its id, time, notification block and custom data`() {
        val notification = mockk<RemoteMessage.Notification> {
            every { title } returns "Block title"
            every { body } returns "Block body"
        }
        val message = mockk<RemoteMessage> {
            every { messageId } returns "0:123%abc"
            every { sentTime } returns 1_700_000_000_000
            every { this@mockk.notification } returns notification
            every { data } returns mapOf("title" to "T", "message" to "M")
        }

        val payload = PushPayloadExtractor.from(message)

        assertEquals("0:123%abc", payload.messageId)
        assertEquals(1_700_000_000_000, payload.sentTimeMillis)
        assertEquals("Block title", payload.notificationTitle)
        assertEquals("Block body", payload.notificationBody)
        assertEquals(mapOf("title" to "T", "message" to "M"), payload.data)
    }

    @Test
    fun `a message without a notification block or a sent time has neither`() {
        val message = mockk<RemoteMessage> {
            every { messageId } returns null
            every { sentTime } returns 0L
            every { notification } returns null
            every { data } returns emptyMap()
        }

        val payload = PushPayloadExtractor.from(message)

        assertNull(payload.messageId)
        assertNull(payload.sentTimeMillis)
        assertNull(payload.notificationTitle)
        assertNull(payload.notificationBody)
    }

    @Test
    fun `a tapped tray notification yields the custom data without the FCM bookkeeping`() {
        val intent = trayIntent(
            "google.message_id" to "0:999%xyz",
            "google.sent_time" to 1_700_000_000_000L,
            "google.c.a.e" to "1",
            "gcm.n.e" to "1",
            "from" to "1234",
            "collapse_key" to "br.com.brunocarvalhs.howmuch",
            "title" to "Black Friday",
            "message" to "Up to 30% off"
        )

        val payload = PushPayloadExtractor.from(intent)

        assertNotNull(payload)
        assertEquals("0:999%xyz", payload!!.messageId)
        assertEquals(1_700_000_000_000, payload.sentTimeMillis)
        assertNull(payload.notificationTitle)
        assertEquals(mapOf("title" to "Black Friday", "message" to "Up to 30% off"), payload.data)
    }

    @Test
    fun `a sent time delivered as a string is understood`() {
        val intent = trayIntent("google.message_id" to "m", "google.sent_time" to "1700000000000")

        assertEquals(1_700_000_000_000, PushPayloadExtractor.from(intent)!!.sentTimeMillis)
    }

    @Test
    fun `an intent with only a sent time is still an FCM intent`() {
        val intent = trayIntent("google.sent_time" to 5L)

        val payload = PushPayloadExtractor.from(intent)

        assertNotNull(payload)
        assertNull(payload!!.messageId)
    }

    @Test
    fun `a normal launch is not a push`() {
        assertNull(PushPayloadExtractor.from(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `an invite link is not a push`() {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://how-much-2a72e.web.app/join/ABC"))

        assertNull(PushPayloadExtractor.from(intent))
    }
}
