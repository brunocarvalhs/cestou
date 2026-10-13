package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingJoinUseCaseTest {

    private val texts = mockk<ShoppingTexts> {
        every { listJoinedTitle() } returns "New member"
        every { listJoinedMessage(any(), any()) } answers { "${firstArg<String?>()} joined ${secondArg<String>()}" }
    }
    private val repository = mockk<ShoppingRepository>()
    private val authService = mockk<AuthService>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val useCase = ShoppingJoinUseCase(texts, repository, authService, notificationRepository)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = listOf("owner1"),
        roles = emptyMap()
    )

    @Test
    fun `invoke resolves the short code, joins, then reads the list`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.resolveShortCode("ABC123") } returns "list1"
        coEvery { repository.join(any(), any()) } returns Unit
        coEvery { repository.getById("list1") } returns shopping

        val result = useCase("abc123")

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.resolveShortCode("ABC123")
            repository.join("list1", "u1")
            repository.getById("list1")
        }
        coVerify {
            notificationRepository.notify("owner1", "New member", "null joined Weekly Groceries", "list_joined")
        }
    }

    @Test
    fun `invoke joins by full id before reading it when the token is long`() = runTest {
        val longToken = "a-very-long-shopping-list-id"
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.join(any(), any()) } returns Unit
        coEvery { repository.getById(longToken) } returns shopping

        val result = useCase(longToken)

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.join(longToken, "u1")
            repository.getById(longToken)
        }
    }

    @Test
    fun `invoke fails without joining when the short code is unknown`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.resolveShortCode("ABC123") } returns null

        val result = useCase("abc123")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.join(any(), any()) }
    }

    @Test
    fun `invoke fails when the list can't be read after joining`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.resolveShortCode("ABC123") } returns "list1"
        coEvery { repository.join(any(), any()) } returns Unit
        coEvery { repository.getById("list1") } returns null

        val result = useCase("abc123")

        assertTrue(result.isFailure)
    }
}
