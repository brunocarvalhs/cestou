package br.com.brunocarvalhs.domain.usecases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.usecases.cart.ShareShoppingCartUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareShoppingCartUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = ShareShoppingCartUseCase(repository)

    @Test
    fun `invoke should return token when shopping cart is found`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val token = "shareToken"
        val cart: ShoppingCart = mockk {
            every { id } returns cartId
            every { this@mockk.token } returns token
            every { products } returns mutableListOf()
        }

        coEvery { repository.findById(cartId) } returns cart

        // Act
        val result = useCase(cartId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(token, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when shopping cart is not found`() = runBlocking {
        // Arrange
        val cartId = "cart123"

        coEvery { repository.findById(cartId) } returns null

        // Act
        val result = useCase(cartId)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ShoppingCartNotFoundException)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val exception = RuntimeException("Repository error")

        coEvery { repository.findById(cartId) } throws exception

        // Act
        val result = useCase(cartId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
