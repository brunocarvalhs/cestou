package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.InvalidTokenException
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

class EnterShoppingCartWithTokenUseCaseTest {

    private lateinit var repository: ShoppingCartRepository
    private lateinit var useCase: EnterShoppingCartWithTokenUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = EnterShoppingCartWithTokenUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should return ShoppingCart on success when token is valid`() = runBlocking {
        // Arrange
        val validToken = "valid-token"
        val expectedShoppingCart = mockk<ShoppingCart>()
        coEvery { repository.findByToken(validToken) } returns expectedShoppingCart

        // Act
        val result = useCase(validToken)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedShoppingCart, result.getOrNull())
        coVerify(exactly = 1) { repository.findByToken(validToken) }
    }

    @Test
    fun `invoke should return failure with InvalidTokenException when token is not found`() = runBlocking {
        // Arrange
        val invalidToken = "invalid-token"
        coEvery { repository.findByToken(invalidToken) } returns null

        // Act
        val result = useCase(invalidToken)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidTokenException)
        coVerify(exactly = 1) { repository.findByToken(invalidToken) }
    }

    @Test
    fun `invoke should return failure when repository throws an exception`() = runBlocking {
        // Arrange
        val anyToken = "any-token"
        val repositoryException = RuntimeException("Database error")
        coEvery { repository.findByToken(anyToken) } throws repositoryException

        // Act
        val result = useCase(anyToken)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(repositoryException, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.findByToken(anyToken) }
    }
}