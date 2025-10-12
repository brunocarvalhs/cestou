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

class CreateNewCartUseCaseTest {

    private lateinit var repository: ShoppingCartRepository
    private lateinit var useCase: CreateNewCartUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreateNewCartUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should call repository to create cart successfully`() = runBlocking {
        // Given
        coEvery { repository.createNewCart() } returns Unit

        // When
        useCase.invoke()

        // Then
        coVerify(exactly = 1) { repository.createNewCart() }
    }

    @Test
    fun `invoke should throw exception when repository fails`() = runBlocking {
        // Given (Dado)
        val repositoryException = Exception("Failed to access database")
        coEvery { repository.createNewCart() } throws repositoryException

        // When/Then
        assertThrows(Exception::class.java) {
            runBlocking {
                useCase.invoke()
            }
        }

        // Then
        coVerify(exactly = 1) { repository.createNewCart() }
    }
}