package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.entity.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.service.AuthService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingCreateUseCaseTest {

    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<ShoppingRepository>(relaxed = true)
    private val authService = mockk<AuthService>()
    private val useCase = ShoppingCreateUseCase(context, repository, authService)

    @Test
    fun `invoke should create shopping list with current user as owner`() = runTest {
        // Given
        val userId = "user-123"
        val user = AuthenticatedUser(id = userId, email = "test@test.com")
        coEvery { authService.getOrCreateUserId() } returns user

        // When
        val title = "Weekly Groceries"
        val description = "Buy milk and eggs"
        val result = useCase(title, description)

        // Then
        assertTrue(result.isSuccess)
        val shopping = result.getOrNull()
        assertEquals(title, shopping?.title)
        assertEquals(description, shopping?.description)
        assertEquals(listOf(userId), shopping?.users)
        assertEquals("OWNER", shopping?.roles?.get(userId))
        
        coVerify { repository.create(any()) }
    }
}
