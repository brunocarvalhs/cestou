package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetByIdUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingGetByIdUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val useCase = ShoppingGetByIdUseCase(repository)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `invoke returns the shopping list when found`() = runTest {
        coEvery { repository.getById("list1") } returns shopping

        val result = useCase("list1")

        assertTrue(result.isSuccess)
        assertEquals(shopping, result.getOrNull())
    }

    @Test
    fun `invoke fails when the shopping list is not found`() = runTest {
        coEvery { repository.getById("missing") } returns null

        val result = useCase("missing")

        assertTrue(result.isFailure)
    }
}
