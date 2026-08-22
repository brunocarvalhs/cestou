package br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingDeleteUseCaseTest {

    private val repository = mockk<ShoppingRepository>(relaxed = true)
    private val useCase = ShoppingDeleteUseCase(repository)

    @Test
    fun `invoke should delete shopping list when found`() = runTest {
        // Given
        val shoppingId = "list-1"
        val shopping = mockk<Shopping>()
        coEvery { repository.getById(shoppingId) } returns shopping

        // When
        val result = useCase(shoppingId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.delete(shopping) }
    }

    @Test
    fun `invoke should return failure when shopping list not found`() = runTest {
        // Given
        val shoppingId = "non-existent"
        coEvery { repository.getById(shoppingId) } returns null

        // When
        val result = useCase(shoppingId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Shopping not found", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.delete(any()) }
    }
}
