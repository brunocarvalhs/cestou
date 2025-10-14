package br.com.brunocarvalhs.domain.usecases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.usecases.cart.UpdateShoppingCartUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateShoppingCartUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = UpdateShoppingCartUseCase(repository)

    @Test
    fun `invoke should return updated shopping cart when update is successful`() = runBlocking {
        // Arrange
        val cart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }
        val updatedCart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }

        coEvery { repository.update(cart) } returns updatedCart

        // Act
        val result = useCase(cart)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(updatedCart, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when shopping cart is not found`() = runBlocking {
        // Arrange
        val cart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }

        coEvery { repository.update(cart) } returns null

        // Act
        val result = useCase(cart)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ShoppingCartNotFoundException)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }
        val exception = RuntimeException("Repository error")

        coEvery { repository.update(cart) } throws exception

        // Act
        val result = useCase(cart)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
