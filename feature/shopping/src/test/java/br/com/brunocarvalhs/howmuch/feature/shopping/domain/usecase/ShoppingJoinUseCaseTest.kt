package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingJoinUseCaseTest {

    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<ShoppingRepository>()
    private val authService = mockk<AuthService>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val useCase = ShoppingJoinUseCase(context, repository, authService, notificationRepository)

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
    fun `invoke joins by short code when the token is short`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.getByShortCode("ABC123") } returns shopping
        coEvery { repository.join(any(), any()) } returns Unit

        val result = useCase("abc123")

        assertTrue(result.isSuccess)
        coVerify { repository.getByShortCode("ABC123") }
        coVerify { repository.join("list1", "u1") }
        coVerify { notificationRepository.notify("owner1", any(), any(), "list_joined") }
    }

    @Test
    fun `invoke joins by full id when the token is long`() = runTest {
        val longToken = "a-very-long-shopping-list-id"
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.getById(longToken) } returns shopping
        coEvery { repository.join(any(), any()) } returns Unit

        val result = useCase(longToken)

        assertTrue(result.isSuccess)
        coVerify { repository.getById(longToken) }
    }

    @Test
    fun `invoke fails when no shopping list matches the token`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "u1", email = "u@test.com")
        coEvery { repository.getByShortCode("ABC123") } returns null

        val result = useCase("abc123")

        assertTrue(result.isFailure)
    }
}
