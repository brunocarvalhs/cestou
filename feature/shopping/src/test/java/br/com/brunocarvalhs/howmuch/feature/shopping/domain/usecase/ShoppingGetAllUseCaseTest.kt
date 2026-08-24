package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetAllUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingGetAllUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val useCase = ShoppingGetAllUseCase(repository)

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
    fun `invoke returns every shopping list from the repository`() = runTest {
        coEvery { repository.getAll() } returns listOf(shopping)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(listOf(shopping), result.getOrNull())
    }

    @Test
    fun `invoke fails when the repository throws`() = runTest {
        coEvery { repository.getAll() } throws IllegalStateException("boom")

        val result = useCase()

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute (AI agent entry point) delegates to invoke`() = runTest {
        coEvery { repository.getAll() } returns listOf(shopping)

        val result = useCase.execute(emptyMap(), mockk(relaxed = true), emptyMap())

        assertEquals(listOf(shopping), result.getOrNull())
    }
}
