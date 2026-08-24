package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetDetailsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingGetDetailsUseCaseTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val useCase = ShoppingGetDetailsUseCase(shoppingRepository, productsUseCase)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "desc",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap(),
        budget = 100.0
    )

    @Test
    fun `execute reads shoppingId from arguments and returns totals`() = runTest {
        coEvery { shoppingRepository.getById("list1") } returns shopping
        coEvery { productsUseCase("list1") } returns flowOf(
            listOf(Product(id = "p1", name = "Milk", quantity = 2.0, price = 5.0))
        )

        val result = useCase.execute(mapOf("shoppingId" to "list1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isSuccess)
        val details = result.getOrNull()!!
        assertEquals("Weekly Groceries", details["title"])
        assertEquals(10.0, details["totalSpent"])
    }

    @Test
    fun `execute falls back to shoppingId from metadata when arguments have none`() = runTest {
        coEvery { shoppingRepository.getById("list1") } returns shopping
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())

        val result = useCase.execute(emptyMap(), mockk(relaxed = true), mapOf("shopping" to "list1"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `execute fails when no shoppingId is available anywhere`() = runTest {
        val result = useCase.execute(emptyMap(), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when the shopping list is not found`() = runTest {
        coEvery { shoppingRepository.getById("missing") } returns null

        val result = useCase.execute(mapOf("shoppingId" to "missing"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }
}
