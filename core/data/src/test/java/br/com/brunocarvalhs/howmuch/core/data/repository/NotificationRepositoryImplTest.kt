package br.com.brunocarvalhs.howmuch.core.data.repository

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationOrigin
import br.com.brunocarvalhs.howmuch.core.domain.repository.SaveResult
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val repository = NotificationRepositoryImpl(networkService)

    @Test
    fun `observeNotifications maps network models to domain notifications`() = runTest {
        val model = NotificationModel(
            id = "n1",
            userId = "u1",
            title = "Lista compartilhada",
            message = "Fulano compartilhou uma lista",
            type = "share",
            isRead = false,
            timestamp = 1000L
        )
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flowOf(listOf(model))

        repository.observeNotifications("u1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("n1", result.first().id)
            assertEquals("Lista compartilhada", result.first().title)
            awaitComplete()
        }
    }

    @Test
    fun `observeNotifications emits empty list when network returns null`() = runTest {
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flowOf(null)

        repository.observeNotifications("u1").test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `markAsRead succeeds when network call succeeds`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        val result = repository.markAsRead("n1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `markAsRead fails when network call throws`() = runTest {
        coEvery {
            networkService.make<Boolean>(any(), any(), any())
        } throws NetworkService.NetworkException(code = 500)

        val result = repository.markAsRead("n1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `notify succeeds when network call succeeds`() = runTest {
        coEvery { networkService.make<String>(any(), any(), any()) } returns "n2"

        val result = repository.notify("u2", "Title", "Message", "list_joined")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `notify fails when network call throws`() = runTest {
        coEvery {
            networkService.make<String>(any(), any(), any())
        } throws NetworkService.NetworkException(code = 403)

        val result = repository.notify("u2", "Title", "Message", "list_joined")

        assertTrue(result.isFailure)
    }

    @Test
    fun `observeNotifications maps shoppingId and senderId when present`() = runTest {
        val model = NotificationModel(
            id = "n1", userId = "u1", title = "t", message = "m", type = "list_joined",
            isRead = false, timestamp = 1L, shoppingId = "s1", senderId = "u2"
        )
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flowOf(listOf(model))

        repository.observeNotifications("u1").test {
            val item = awaitItem().single()
            assertEquals("s1", item.shoppingId)
            assertEquals("u2", item.senderId)
            awaitComplete()
        }
    }

    @Test
    fun `observeNotifications keeps shoppingId and senderId null for legacy documents`() = runTest {
        val legacy = NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L)
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flowOf(listOf(legacy))

        repository.observeNotifications("u1").test {
            val item = awaitItem().single()
            assertNull(item.shoppingId)
            assertNull(item.senderId)
            awaitComplete()
        }
    }

    @Test
    fun `observeUnreadCount queries only unread notifications of the user`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        val models = listOf(
            NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L),
            NotificationModel("n2", "u1", "t", "m", "list_finished", false, 2L)
        )
        every {
            networkService.observe<List<NotificationModel>>(capture(request), any(), any())
        } returns flowOf(models)

        repository.observeUnreadCount("u1").test {
            assertEquals(2, awaitItem())
            awaitComplete()
        }
        assertEquals(mapOf("userId" to "u1", "isRead" to false), request.captured.query)
    }

    @Test
    fun `observeUnreadCount emits zero when network returns null`() = runTest {
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flowOf(null)

        repository.observeUnreadCount("u1").test {
            assertEquals(0, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeUnreadCount emits zero when the listener fails`() = runTest {
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flow { throw NetworkService.NetworkException(code = 403) }

        repository.observeUnreadCount("u1").test {
            assertEquals(0, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `markAllAsRead does nothing for an empty list`() = runTest {
        val result = repository.markAllAsRead(emptyList())

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { networkService.make<Boolean>(any(), any(), any()) }
    }

    @Test
    fun `markAllAsRead updates every id`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        val ids = (1..25).map { "n$it" }
        val result = repository.markAllAsRead(ids)

        assertTrue(result.isSuccess)
        coVerify(exactly = 25) { networkService.make<Boolean>(any(), any(), any()) }
    }

    @Test
    fun `markAllAsRead reports a failure but still attempts the remaining ids`() = runTest {
        coEvery {
            networkService.make<Boolean>(match { it.endpoint == "notifications/n1" }, any(), any())
        } throws NetworkService.NetworkException(code = 500)
        coEvery {
            networkService.make<Boolean>(match { it.endpoint != "notifications/n1" }, any(), any())
        } returns true

        val result = repository.markAllAsRead(listOf("n1", "n2", "n3"))

        assertTrue(result.isFailure)
        coVerify(exactly = 3) { networkService.make<Boolean>(any(), any(), any()) }
    }

    @Test
    fun `notify sends shoppingId and senderId when provided`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        coEvery { networkService.make<String>(capture(request), any(), any()) } returns "n2"

        repository.notify(
            "u2",
            "Title",
            "Message",
            "list_joined",
            NotificationOrigin(shoppingId = "s1", senderId = "u1")
        )

        assertEquals("s1", request.captured.payload?.get("shoppingId"))
        assertEquals("u1", request.captured.payload?.get("senderId"))
    }

    @Test
    fun `notify omits shoppingId and senderId keys when absent`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        coEvery { networkService.make<String>(capture(request), any(), any()) } returns "n2"

        repository.notify("u2", "Title", "Message", "list_joined")

        val payload = requireNotNull(request.captured.payload)
        assertFalse(payload.containsKey("shoppingId"))
        assertFalse(payload.containsKey("senderId"))
    }

    @Test
    fun `observeNotifications resubscribes after a listener failure`() = runTest {
        var subscriptions = 0
        val model = NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L)
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flow {
            subscriptions++
            if (subscriptions == 1) throw NetworkService.NetworkException(code = 14)
            emit(listOf(model))
        }

        repository.observeNotifications("u1").test {
            assertEquals("n1", awaitItem().single().id)
            awaitComplete()
        }
        assertEquals(2, subscriptions)
    }

    @Test
    fun `observeUnreadCount resubscribes after a listener failure`() = runTest {
        var subscriptions = 0
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flow {
            subscriptions++
            if (subscriptions == 1) throw NetworkService.NetworkException(code = 14)
            emit(listOf(NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L)))
        }

        repository.observeUnreadCount("u1").test {
            assertEquals(1, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeNotifications gives up after the retry budget and emits empty`() = runTest {
        var subscriptions = 0
        every {
            networkService.observe<List<NotificationModel>>(any(), any(), any())
        } returns flow {
            subscriptions++
            throw NetworkService.NetworkException(code = 7)
        }

        repository.observeNotifications("u1").test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
        assertEquals(4, subscriptions)
    }

    @Test
    fun `markAsRead keeps a cancelled coroutine cancelled`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.markAsRead("n1") }

        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `notify keeps a cancelled coroutine cancelled`() = runTest {
        coEvery { networkService.make<String>(any(), any(), any()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.notify("u2", "t", "m", "list_joined") }

        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }

    private fun campaign() = Notification(
        id = "fcm-1",
        userId = "u1",
        title = "t",
        message = "m",
        type = "campaign",
        isRead = false,
        timestamp = 5L
    )

    @Test
    fun `save posts the notification under its own id`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        coEvery { networkService.make<String>(capture(request), any(), any()) } returns "fcm-1"

        val result = repository.save(campaign())

        assertEquals(SaveResult.Saved, result.getOrNull())
        assertEquals(NetworkService.Method.POST, request.captured.method)
        assertEquals("notifications", request.captured.endpoint)
        assertEquals("fcm-1", request.captured.payload?.get("id"))
        assertEquals("campaign", request.captured.payload?.get("type"))
        assertFalse(request.captured.payload!!.containsKey("shoppingId"))
    }

    @Test
    fun `save keeps a cancelled coroutine cancelled`() = runTest {
        coEvery { networkService.make<String>(any(), any(), any()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.save(campaign()) }

        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }
}
