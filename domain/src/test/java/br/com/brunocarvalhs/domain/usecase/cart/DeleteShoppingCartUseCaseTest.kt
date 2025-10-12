package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeleteShoppingCartUseCaseTest {

    private lateinit var repository: ShoppingCartRepository
    private lateinit var useCase: DeleteShoppingCartUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = DeleteShoppingCartUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should return success when repository deletes successfully`() = runBlocking {
        // Arrange
        val cartId = "test-cart-id"
        coEvery { repository.delete(cartId) } returns Unit

        // Act
        val result = useCase(cartId)

        // Assert
        coVerify(exactly = 1) { repository.delete(cartId) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val cartId = "test-cart-id"
        val exception = RuntimeException("Failed to delete")
        coEvery { repository.delete(cartId) } throws exception

        // Act
        val result = useCase(cartId)

        // Assert
        coVerify(exactly = 1) { repository.delete(cartId) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertTrue(result.exceptionOrNull()?.message == "Failed to delete")
    }
}