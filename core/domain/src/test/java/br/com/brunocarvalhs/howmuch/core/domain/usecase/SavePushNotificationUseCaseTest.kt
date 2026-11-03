package br.com.brunocarvalhs.howmuch.core.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.PushPayload
import br.com.brunocarvalhs.howmuch.core.domain.model.PushSaveOutcome
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SaveResult
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SavePushNotificationUseCaseTest {

    private val now = Instant.parse("2026-11-02T12:00:00Z")
    private val authService = mockk<AuthService>()
    private val repository = mockk<NotificationRepository>()
    private val saved = slot<Notification>()
    private val useCase = SavePushNotificationUseCase(authService, repository, Clock.fixed(now, ZoneOffset.UTC))

    @Before
    fun setup() {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        coEvery { repository.save(capture(saved)) } returns Result.success(SaveResult.Saved)
    }

    private fun payload(
        data: Map<String, String> = mapOf("title" to "Black Friday", "message" to "Up to 30% off"),
        messageId: String? = "0:1700%abc",
        sentTime: Long? = 1_700_000_000_000
    ) = PushPayload(messageId, sentTime, notificationTitle = null, notificationBody = null, data = data)

    @Test
    fun `completes the missing fields for the signed-in user`() = runTest {
        val outcome = useCase(payload())

        assertEquals(PushSaveOutcome.Saved, outcome)
        with(saved.captured) {
            assertEquals("fcm-0_1700_abc", id)
            assertEquals("u1", userId)
            assertEquals("Black Friday", title)
            assertEquals("Up to 30% off", message)
            assertEquals("campaign", type)
            assertFalse(isRead)
            assertEquals(1_700_000_000_000, timestamp)
        }
    }

    @Test
    fun `leaves the list and sender empty`() = runTest {
        useCase(payload())

        assertEquals(null, saved.captured.shoppingId)
        assertEquals(null, saved.captured.senderId)
    }

    @Test
    fun `uses the clock when FCM sent no time`() = runTest {
        useCase(payload(sentTime = null))

        assertEquals(now.toEpochMilli(), saved.captured.timestamp)
    }

    @Test
    fun `falls back to the notification block when the custom data is missing`() = runTest {
        val outcome = useCase(
            PushPayload("m1", null, notificationTitle = "From block", notificationBody = "Body", data = emptyMap())
        )

        assertEquals(PushSaveOutcome.Saved, outcome)
        assertEquals("From block", saved.captured.title)
        assertEquals("Body", saved.captured.message)
    }

    @Test
    fun `custom data wins over the notification block`() = runTest {
        useCase(
            PushPayload(
                "m1",
                null,
                notificationTitle = "Block",
                notificationBody = "Block body",
                data = mapOf("title" to "Data", "message" to "Data body")
            )
        )

        assertEquals("Data", saved.captured.title)
        assertEquals("Data body", saved.captured.message)
    }

    @Test
    fun `trims and cuts the text to the limits the Firestore rules accept`() = runTest {
        useCase(payload(data = mapOf("title" to "  " + "t".repeat(300), "message" to "m".repeat(900))))

        assertEquals(200, saved.captured.title.length)
        assertEquals(500, saved.captured.message.length)
        assertTrue(saved.captured.title.all { it == 't' })
    }

    @Test
    fun `a missing or blank title or message is ignored`() = runTest {
        val noTitle = useCase(payload(data = mapOf("message" to "only message")))
        val blankMessage = useCase(payload(data = mapOf("title" to "only title", "message" to "   ")))
        val nothing = useCase(payload(data = emptyMap()))

        listOf(noTitle, blankMessage, nothing).forEach {
            assertEquals(PushSaveOutcome.Ignored(PushSaveOutcome.IgnoreReason.EMPTY), it)
        }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `nobody signed in is ignored without touching the repository`() = runTest {
        every { authService.currentUser } returns null

        val outcome = useCase(payload())

        assertEquals(PushSaveOutcome.Ignored(PushSaveOutcome.IgnoreReason.NO_USER), outcome)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `the same FCM message always maps to the same document id`() = runTest {
        useCase(payload(messageId = "msg/1"))
        val first = saved.captured.id
        useCase(payload(messageId = "msg/1"))

        assertEquals(first, saved.captured.id)
        assertEquals("fcm-msg_1", first)
    }

    @Test
    fun `a message without an id gets a random one`() = runTest {
        useCase(payload(messageId = null))
        val first = saved.captured.id
        useCase(payload(messageId = " "))
        val second = saved.captured.id

        assertTrue(first.startsWith("fcm-"))
        assertTrue(first != second)
    }

    @Test
    fun `a very long message id is capped`() = runTest {
        useCase(payload(messageId = "x".repeat(500)))

        assertEquals("fcm-".length + 100, saved.captured.id.length)
    }

    @Test
    fun `a document that already exists is reported as already saved`() = runTest {
        coEvery { repository.save(any()) } returns Result.success(SaveResult.AlreadySaved)

        assertEquals(PushSaveOutcome.AlreadySaved, useCase(payload()))
    }

    @Test
    fun `a repository failure is reported with its cause`() = runTest {
        val error = IllegalStateException("offline")
        coEvery { repository.save(any()) } returns Result.failure(error)

        val outcome = useCase(payload())

        assertEquals(PushSaveOutcome.Failed(error), outcome)
    }
}
