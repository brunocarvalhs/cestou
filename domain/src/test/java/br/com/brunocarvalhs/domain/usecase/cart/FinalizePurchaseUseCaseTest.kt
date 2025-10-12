package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FinalizePurchaseUseCaseTest {

    private lateinit var repository: ShoppingCartRepository
    private lateinit var createNewCartUseCase: CreateNewCartUseCase
    private lateinit var useCase: FinalizePurchaseUseCase

    private val initialCart: ShoppingCart = mockk()
    private val finalizedCart: ShoppingCart = mockk()

    @Before
    fun setUp() {
        repository = mockk()
        createNewCartUseCase = mockk()
        useCase = FinalizePurchaseUseCase(repository, createNewCartUseCase)

        coEvery { initialCart.finalizePurchase(any<String>(), any<Long>()) } returns finalizedCart
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should run successfully and create new cart when everything is correct`() = runBlocking {
        // Arrange
        val cartId = "valid-cart-id"
        val market = "Test Market"
        val price = 1000L

        coEvery { repository.findById(cartId) } returns initialCart
        coEvery { repository.update(finalizedCart) } just runs
        coEvery { createNewCartUseCase.invoke() } returns Result.success(mockk())

        // Act
        val result = useCase(cartId, market, price)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.findById(cartId) }
        coVerify(exactly = 1) { initialCart.finalizePurchase(market, price) }
        coVerify(exactly = 1) { repository.update(finalizedCart) }
        coVerify(exactly = 1) { createNewCartUseCase() }
    }

    @Test
    fun `invoke should return early when cartId is null`() = runBlocking {
        // Arrange
        val market = "Test Market"
        val price = 1000L

        // Act
        val result = useCase(null, market, price)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { repository.findById(any()) }
        coVerify(exactly = 0) { repository.update(any()) }
        coVerify(exactly = 0) { createNewCartUseCase() }
    }

    @Test
    fun `invoke should return early when cart is not found`() = runBlocking {
        // Arrange
        val cartId = "not-found-id"
        val market = "Test Market"
        val price = 1000L

        coEvery { repository.findById(cartId) } returns null

        // Act
        val result = useCase(cartId, market, price)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.findById(cartId) }
        coVerify(exactly = 0) { repository.update(any()) }
        coVerify(exactly = 0) { createNewCartUseCase() }
    }

    @Test
    fun `invoke should return failure when findById throws exception`() = runBlocking {
        // Arrange
        val cartId = "valid-cart-id"
        val exception = RuntimeException("DB error")
        coEvery { repository.findById(cartId) } throws exception

        // Act
        val result = useCase(cartId, "market", 100L)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.findById(cartId) }
        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `invoke should return failure when update throws exception`() = runBlocking {
        // Arrange
        val cartId = "valid-cart-id"
        val exception = RuntimeException("Update failed")
        coEvery { repository.findById(cartId) } returns initialCart
        coEvery { repository.update(finalizedCart) } throws exception

        // Act
        val result = useCase(cartId, "market", 100L)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.update(finalizedCart) }
        coVerify(exactly = 0) { createNewCartUseCase() }
    }

    @Test
    fun `invoke should return failure when createNewCartUseCase throws exception`() = runBlocking {
        // Arrange
        val cartId = "valid-cart-id"
        val exception = RuntimeException("Failed to create new cart")
        coEvery { repository.findById(cartId) } returns initialCart
        coEvery { repository.update(finalizedCart) } just runs
        coEvery { createNewCartUseCase() } throws exception

        // Act
        val result = useCase(cartId, "market", 100L)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.update(finalizedCart) }
        coVerify(exactly = 1) { createNewCartUseCase() }
    }
}