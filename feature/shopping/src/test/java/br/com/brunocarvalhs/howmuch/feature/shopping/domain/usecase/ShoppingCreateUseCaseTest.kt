package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        assertEquals(Shopping.DEFAULT_EMOJI, shopping?.emoji)

        coVerify { repository.create(any()) }
    }

    @Test
    fun `invoke should create shopping list with the given emoji`() = runTest {
        val userId = "user-123"
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = userId, email = "test@test.com")

        val result = useCase(title = "Weekly Groceries", emoji = "🍕")

        assertEquals("🍕", result.getOrNull()?.emoji)
    }

    @Test
    fun `invoke falls back to the default title and description strings when the AI agent omits them`() = runTest {
        val userId = "user-123"
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = userId, email = "test@test.com")
        every { context.getString(R.string.shopping_list_new_title) } returns "Nova lista"
        every { context.getString(R.string.shopping_list_new_description) } returns "Lista criada pelo assistente"

        val result = useCase(title = null, description = null)

        assertTrue(result.isSuccess)
        assertEquals("Nova lista", result.getOrNull()?.title)
        assertEquals("Lista criada pelo assistente", result.getOrNull()?.description)
        verify { context.getString(R.string.shopping_list_new_title) }
        verify { context.getString(R.string.shopping_list_new_description) }
    }

    @Test
    fun `execute creates a shopping list from AI agent arguments`() = runTest {
        val userId = "user-123"
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = userId, email = "test@test.com")

        val result = useCase.execute(
            arguments = mapOf("title" to "Churrasco", "description" to "Amigos no sabado"),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        assertEquals("Churrasco", result.getOrNull()?.title)
        assertEquals("Amigos no sabado", result.getOrNull()?.description)
        coVerify { repository.create(any()) }
    }
}
