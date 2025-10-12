package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CreateShoppingCartUseCaseTest {

    private lateinit var repository: ShoppingCartRepository
    private lateinit var useCase: CreateShoppingCartUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreateShoppingCartUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should create shopping cart with success`() = runBlocking {
        // Arrange
        val cartId = "123"
        val cart = mockk<ShoppingCart>()
        val idSlot = slot<String>()

        every { cart.id } returns cartId
        coEvery { repository.create(cart) } returns Unit
        coEvery { repository.findById(capture(idSlot)) } returns cart

        // Act
        val result = useCase.invoke(cart)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(cart, result.getOrNull())
        assertEquals(cartId, idSlot.captured)

        coVerify(exactly = 1) { repository.create(cart) }
        coVerify(exactly = 1) { repository.findById(cartId) }
    }

    @Test
    fun `should return failure when repository fails to create cart`() = runBlocking {
        // Arrange
        val cart = mockk<ShoppingCart>()
        val exception = Exception("Failed to create cart")
        coEvery { repository.create(cart) } throws exception

        // Act
        val result = useCase.invoke(cart)

        // Assert
        assertFalse(result.isSuccess)
    }

    @Test
    fun `should return failure when repository fails to find created cart`() = runBlocking {
        // Arrange
        val cart = mockk<ShoppingCart>()
        coEvery { repository.create(cart) } just runs
        coEvery { repository.findById(cart.id) } returns null

        // Act
        val result = useCase.invoke(cart)

        // Assert
        assertFalse(result.isSuccess)
    }
}