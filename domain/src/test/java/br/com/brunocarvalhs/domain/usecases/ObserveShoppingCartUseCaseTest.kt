package br.com.brunocarvalhs.domain.usecases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.usecases.cart.ObserveShoppingCartUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveShoppingCartUseCaseTest {

    private val repository: ShoppingCartRepository = mockk()
    private val useCase = ObserveShoppingCartUseCase(repository)

    @Test
    fun `invoke should return a flow of shopping cart`() = runBlocking {
        // Arrange
        val cartId = "cart123"
        val shoppingCart: ShoppingCart = mockk {
            every { id } returns cartId
            every { products } returns mutableListOf()
        }
        val cartFlow: Flow<ShoppingCart> = flowOf(shoppingCart)

        every { repository.observeCart(cartId) } returns cartFlow

        // Act
        val result = useCase(cartId)

        // Assert
        assertEquals(cartFlow, result)
    }
}
