package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetHistoryCartUseCaseTest {

    private val localStorage: ICartLocalStorage = mockk()
    private val useCase = GetHistoryCartUseCase(localStorage)

    @Test
    fun `invoke should return cart history successfully`() = runBlocking {
        // Arrange
        val cartHistory = listOf(
            mockk<ShoppingCart> {
                every { id } returns "cart1"
                every { products } returns mutableListOf()
            },
            mockk<ShoppingCart> {
                every { id } returns "cart2"
                every { products } returns mutableListOf()
            }
        )

        coEvery { localStorage.getCartHistory() } returns cartHistory

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(cartHistory, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when local storage throws an exception`() = runBlocking {
        // Arrange
        val exception = RuntimeException("Local storage error")

        coEvery { localStorage.getCartHistory() } throws exception

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
