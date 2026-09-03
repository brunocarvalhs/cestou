package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductListSaveUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductListSaveUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductListSaveUseCase(repository)

    @Test
    fun `invoke saves every product and returns them all on success`() = runTest {
        val p1 = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        val p2 = Product(id = "p2", name = "Eggs", quantity = 1.0, price = 2.0)
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase(listOf(p1, p2), "list1")

        assertTrue(result.isSuccess)
        assertEquals(listOf(p1, p2), result.getOrNull())
        coVerify { repository.saveProduct(p1, "list1") }
        coVerify { repository.saveProduct(p2, "list1") }
    }

    @Test
    fun `invoke stops and fails on the first save error`() = runTest {
        val p1 = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        val p2 = Product(id = "p2", name = "Eggs", quantity = 1.0, price = 2.0)
        val error = IllegalStateException("save failed")
        coEvery { repository.saveProduct(p1, "list1") } returns Result.failure(error)

        val result = useCase(listOf(p1, p2), "list1")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.saveProduct(p2, "list1") }
    }

    @Test
    fun `execute parses comma-separated product names and saves them`() = runTest {
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase.execute(
            mapOf("productNames" to "Milk, Eggs", "shoppingId" to "list1"),
            mockk(relaxed = true),
            emptyMap()
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("Milk", "Eggs"), result.getOrNull()?.map { it.name })
    }

    @Test
    fun `execute fails when productNames is missing`() = runTest {
        val result = useCase.execute(mapOf("shoppingId" to "list1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when shoppingId is missing from both arguments and metadata`() = runTest {
        val result = useCase.execute(mapOf("productNames" to "Milk"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }
}
