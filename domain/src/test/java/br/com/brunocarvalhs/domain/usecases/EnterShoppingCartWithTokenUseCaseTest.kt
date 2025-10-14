package br.com.brunocarvalhs.domain.usecases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.InvalidTokenException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.usecases.cart.EnterShoppingCartWithTokenUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnterShoppingCartWithTokenUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = EnterShoppingCartWithTokenUseCase(repository)

    @Test
    fun `invoke should return shopping cart when token is valid`() = runBlocking {
        // Arrange
        val token = "validToken"
        val cart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }

        coEvery { repository.findByToken(token) } returns cart

        // Act
        val result = useCase(token)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(cart, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when token is invalid`() = runBlocking {
        // Arrange
        val token = "invalidToken"

        coEvery { repository.findByToken(token) } returns null

        // Act
        val result = useCase(token)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidTokenException)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val token = "validToken"
        val exception = RuntimeException("Repository error")

        coEvery { repository.findByToken(token) } throws exception

        // Act
        val result = useCase(token)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
