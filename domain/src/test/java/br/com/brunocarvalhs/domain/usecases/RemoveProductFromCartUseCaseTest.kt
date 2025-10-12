package br.com.brunocarvalhs.domain.usecases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ProductNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.usecases.product.RemoveProductFromCartUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveProductFromCartUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = RemoveProductFromCartUseCase(repository)

    @Test
    fun `invoke should return updated shopping cart when product is removed successfully`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val productId = "product123"
        val updatedCart: ShoppingCart = mockk {
            every { id } returns cartId
            every { products } returns mutableListOf()
        }

        coEvery { repository.removeProduct(cartId, productId) } returns updatedCart

        // Act
        val result = useCase(cartId, productId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(updatedCart, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when product is not found`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val productId = "product123"

        coEvery { repository.removeProduct(cartId, productId) } returns null

        // Act
        val result = useCase(cartId, productId)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProductNotFoundException)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val productId = "product123"
        val exception = RuntimeException("Repository error")

        coEvery { repository.removeProduct(cartId, productId) } throws exception

        // Act
        val result = useCase(cartId, productId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
