package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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

    @Test
    fun `execute resolves the shopping id from arguments and returns the shopping list`() = runTest {
        coEvery { repository.getById("list1") } returns shopping

        val result = useCase.execute(
            arguments = mapOf("shopping_id" to "list1"),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        assertEquals(shopping, result.getOrNull())
    }

    @Test
    fun `execute throws when shopping_id argument is missing`() {
        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(arguments = emptyMap(), session = mockk(relaxed = true), metadata = emptyMap())
            }
        }
    }
}
