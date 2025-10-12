package br.com.brunocarvalhs.domain.usecases

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.usecases.cart.FinalizePurchaseUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizePurchaseUseCaseTest {

    private val shoppingCartRepository: ShoppingCartRepository = mockk()
    private val localStorage: ICartLocalStorage = mockk(relaxed = true)
    private val useCase = FinalizePurchaseUseCase(shoppingCartRepository, localStorage)

    @Test
    fun `invoke should return failure when shopping cart is not found`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val market = "Test Market"
        val price = 100L

        coEvery { shoppingCartRepository.findById(cartId) } returns null

        // Act
        val result = useCase(cartId, market, price)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val market = "Test Market"
        val price = 100L
        val exception = RuntimeException("Repository error")

        coEvery { shoppingCartRepository.findById(cartId) } throws exception

        // Act
        val result = useCase(cartId, market, price)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() == exception)
    }
}
