package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkAllNotificationsReadUseCaseTest {

    private val repository = mockk<NotificationRepository>()
    private val useCase = MarkAllNotificationsReadUseCase(repository)

    private fun notification(id: String, isRead: Boolean) =
        Notification(id, "u1", "t", "m", "list_joined", isRead, 1L)

    @Test
    fun `sends only the unread ids and reports how many`() = runTest {
        coEvery { repository.markAllAsRead(any()) } returns Result.success(Unit)

        val result = useCase(listOf(notification("a", false), notification("b", true), notification("c", false)))

        assertEquals(2, result.getOrNull())
        coVerify { repository.markAllAsRead(listOf("a", "c")) }
    }

    @Test
    fun `does not touch the repository when everything is already read`() = runTest {
        val result = useCase(listOf(notification("a", true)))

        assertEquals(0, result.getOrNull())
        coVerify(exactly = 0) { repository.markAllAsRead(any()) }
    }

    @Test
    fun `propagates a repository failure`() = runTest {
        coEvery { repository.markAllAsRead(any()) } returns Result.failure(IllegalStateException("boom"))

        val result = useCase(listOf(notification("a", false)))

        assertTrue(result.isFailure)
    }
}
