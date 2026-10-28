package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
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
    fun `invoke writes only the given fields`() = runTest {
        coEvery { repository.updateFields("list1", mapOf("title" to "New title")) } returns Result.success(Unit)

        val result = useCase("list1", mapOf("title" to "New title"))

        assertTrue(result.isSuccess)
        coVerify { repository.updateFields("list1", mapOf("title" to "New title")) }
    }

    @Test
    fun `execute builds a fields map from AI agent arguments and writes it`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)

        val result = useCase.execute(
            arguments = mapOf(
                "shopping_id" to "list1",
                "title" to "Weekly Groceries",
                "description" to "desc",
                "price" to UPDATED_PRICE,
                "status" to "NEW"
            ),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.updateFields(
                "list1",
                mapOf(
                    "title" to "Weekly Groceries",
                    "description" to "desc",
                    "price" to UPDATED_PRICE,
                    "status" to "NEW"
                )
            )
        }
    }

    @Test
    fun `execute throws when shopping_id argument is missing`() {
        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(
                    arguments = emptyMap(),
                    session = mockk(relaxed = true),
                    metadata = emptyMap()
                )
            }
        }
    }

    @Test
    fun `execute throws when the shopping list does not exist`() {
        coEvery { repository.getById("missing") } returns null

        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(
                    arguments = mapOf("shopping_id" to "missing"),
                    session = mockk(relaxed = true),
                    metadata = emptyMap()
                )
            }
        }
    }

    @Test
    fun `execute only sends the field that was actually provided`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)

        val result = useCase.execute(
            arguments = mapOf(
                "shopping_id" to "list1",
                "price" to UPDATED_PRICE
            ),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify { repository.updateFields("list1", mapOf("price" to UPDATED_PRICE)) }
    }
}

private const val UPDATED_PRICE = 10.5
