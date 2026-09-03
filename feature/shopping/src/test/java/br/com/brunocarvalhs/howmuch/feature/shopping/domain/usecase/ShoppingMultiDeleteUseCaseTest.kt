package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingMultiDeleteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingMultiDeleteUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val useCase = ShoppingMultiDeleteUseCase(repository)

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
    fun `invoke deletes the shopping list when it exists`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.delete(shopping) } returns Unit

        val result = useCase("list1")

        assertTrue(result.isSuccess)
        coVerify { repository.delete(shopping) }
    }

    @Test
    fun `invoke fails when the shopping list is not found`() = runTest {
        coEvery { repository.getById("missing") } returns null

        val result = useCase("missing")

        assertTrue(result.isFailure)
    }
}
