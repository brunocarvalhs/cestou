package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class CreateCartUseCaseTest {

    private lateinit var repository: ShoppingCartRepository
    private lateinit var useCase: CreateCartUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreateCartUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should create cart when invoke is called`() = runBlocking {
        // Given
        val cart = mockk<ShoppingCart>(relaxed = true)
        coEvery { repository.create(cart) } returns Unit

        // When
        useCase.invoke(cart)

        // Then (Então)
        coVerify(exactly = 1) { repository.create(cart) }
    }

    @Test
    fun `should propagate exception when repository throws`() = runBlocking {
        // Given
        val cart = mockk<ShoppingCart>(relaxed = true)
        val expectedException = IOException("Repository error")
        coEvery { repository.create(cart) } throws expectedException

        // When / Then
        assertThrows(IOException::class.java) {
            runBlocking {
                useCase.invoke(cart)
            }
        }

        coVerify { repository.create(cart) }
    }
}