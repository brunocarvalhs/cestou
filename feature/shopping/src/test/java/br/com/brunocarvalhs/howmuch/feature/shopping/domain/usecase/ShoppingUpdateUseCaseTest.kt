package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingUpdateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingUpdateUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val useCase = ShoppingUpdateUseCase(repository)

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
    fun `invoke updates the shopping list`() = runTest {
        coEvery { repository.update(shopping) } returns Unit

        val result = useCase(shopping.id, shopping)

        assertTrue(result.isSuccess)
        coVerify { repository.update(shopping) }
    }

    @Test
    fun `execute builds a Shopping from AI agent arguments and updates it`() = runTest {
        coEvery { repository.update(any()) } returns Unit

        val result = useCase.execute(
            arguments = mapOf(
                "shopping_id" to "list1",
                "title" to "Weekly Groceries",
                "description" to "desc",
                "price" to 10.5,
                "status" to "NEW"
            ),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.update(match { it.id == "list1" && it.title == "Weekly Groceries" && it.price == 10.5 })
        }
    }

    @Test
    fun `execute throws when a required argument is missing`() {
        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(
                    arguments = mapOf("shopping_id" to "list1"),
                    session = mockk(relaxed = true),
                    metadata = emptyMap()
                )
            }
        }
    }
}
