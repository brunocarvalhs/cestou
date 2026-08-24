package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingReopenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingReopenUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val useCase = ShoppingReopenUseCase(repository)

    private val finished = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.FINISH,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `invoke updates the shopping list status to IN_PROGRESS`() = runTest {
        coEvery { repository.update(any()) } returns Unit

        val result = useCase(finished)

        assertTrue(result.isSuccess)
        coVerify { repository.update(finished.copy(status = Shopping.Status.IN_PROGRESS)) }
    }

    @Test
    fun `execute looks up the shopping list by id then reopens it`() = runTest {
        coEvery { repository.getById("list1") } returns finished
        coEvery { repository.update(any()) } returns Unit

        val result = useCase.execute(mapOf("shopping_id" to "list1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isSuccess)
        coVerify { repository.update(finished.copy(status = Shopping.Status.IN_PROGRESS)) }
    }

    @Test
    fun `execute throws when the shopping list is not found`() {
        coEvery { repository.getById("missing") } returns null

        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(mapOf("shopping_id" to "missing"), mockk(relaxed = true), emptyMap())
            }
        }
    }
}
