package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddProductUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = AddProductUseCase(repository)

    @Test
    fun `invoke should return updated shopping cart when product is added successfully`() =
        runBlocking {
            // Arrange
            val cartId = "cart123"
            val product: Product = mockk {
                every { id } returns "product123"
                every { name } returns "Test Product"
                every { price } returns 10L
            }
            val updatedCart: ShoppingCart = mockk {
                every { id } returns cartId
                every { products } returns mutableListOf(product)
            }

            coEvery { repository.addProduct(cartId, product) } returns updatedCart

            // Act
            val result = useCase(cartId, product)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(updatedCart, result.getOrNull())
        }

    @Test
    fun `invoke should return failure when shopping cart is not found`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val product: Product = mockk {
            every { id } returns "product123"
            every { name } returns "Test Product"
            every { price } returns 10L
        }

        coEvery { repository.addProduct(cartId, product) } returns null

        // Act
        val result = useCase(cartId, product)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ShoppingCartNotFoundException)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val product: Product = mockk {
            every { id } returns "product123"
            every { name } returns "Test Product"
            every { price } returns 10L
        }
        val exception = RuntimeException("Repository error")

        coEvery { repository.addProduct(cartId, product) } throws exception

        // Act
        val result = useCase(cartId, product)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
