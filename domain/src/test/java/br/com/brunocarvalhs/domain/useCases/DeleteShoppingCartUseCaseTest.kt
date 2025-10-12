package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteShoppingCartUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = DeleteShoppingCartUseCase(repository)

    @Test
    fun `invoke should return success when repository deletes the cart`() = runBlocking {
        // Arrange
        val cartId = "cart123"

        coEvery { repository.delete(cartId) } returns true

        // Act
        val result = useCase(cartId)

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke should return failure when repository fails to delete the cart`() = runBlocking {
        // Arrange
        val cartId = "cart123"

        coEvery { repository.delete(cartId) } returns false

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

        coEvery { repository.delete(cartId) } throws exception

        // Act
        val result = useCase(cartId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
