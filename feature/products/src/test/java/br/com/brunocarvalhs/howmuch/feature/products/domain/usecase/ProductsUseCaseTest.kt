package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductsUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductsUseCase(repository)

    private val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)

    @Test
    fun `invoke returns every product for a shopping list`() = runTest {
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(product))

        assertEquals(listOf(product), useCase("list1").first())
    }

    @Test
    fun `delete forwards to the repository`() = runTest {
        coEvery { repository.deleteProduct("p1", "list1") } returns Result.success(Unit)

        val result = useCase.delete("p1", "list1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `update forwards to the repository`() = runTest {
        coEvery { repository.updateProduct(product, "list1") } returns Result.success(Unit)

        val result = useCase.update(product, "list1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `save forwards to the repository`() = runTest {
        coEvery { repository.saveProduct(product, "list1") } returns Result.success(Unit)

        val result = useCase.save(product, "list1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `move deletes from the current list then saves into the target list`() = runTest {
        coEvery { repository.deleteProduct("p1", "list1") } returns Result.success(Unit)
        coEvery { repository.saveProduct(product, "list2") } returns Result.success(Unit)

        val result = useCase.move(product, "list1", "list2")

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.deleteProduct("p1", "list1")
            repository.saveProduct(product, "list2")
        }
    }

    @Test
    fun `move fails and skips saving when delete fails`() = runTest {
        coEvery { repository.deleteProduct("p1", "list1") } returns Result.failure(IllegalStateException("boom"))

        val result = useCase.move(product, "list1", "list2")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.saveProduct(any(), any()) }
    }
}
