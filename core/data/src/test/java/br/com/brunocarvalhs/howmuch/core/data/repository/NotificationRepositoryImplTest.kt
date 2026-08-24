package br.com.brunocarvalhs.howmuch.core.data.repository

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
