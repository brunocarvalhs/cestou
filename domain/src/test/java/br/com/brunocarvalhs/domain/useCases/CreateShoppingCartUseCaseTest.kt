package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateShoppingCartUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = CreateShoppingCartUseCase(repository)

    @Test
    fun `invoke should return created shopping cart when repository succeeds`() = runBlocking {
        // Arrange
        val cart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }

        coEvery { repository.create(cart) } returns cart

        // Act
        val result = useCase(cart)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(cart, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cart: ShoppingCart = mockk {
            every { id } returns "cart123"
            every { products } returns mutableListOf()
        }
        val exception = RuntimeException("Repository error")

        coEvery { repository.create(cart) } throws exception

        // Act
        val result = useCase(cart)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
