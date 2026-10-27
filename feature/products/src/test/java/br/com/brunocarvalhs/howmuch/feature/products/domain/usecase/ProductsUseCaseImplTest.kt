package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCaseImpl
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

class ProductsUseCaseImplTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductsUseCaseImpl(repository)

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
    fun `move saves into the target list before deleting from the current list`() = runTest {
        coEvery { repository.saveProduct(product, "list2") } returns Result.success(Unit)
        coEvery { repository.deleteProduct("p1", "list1") } returns Result.success(Unit)

        val result = useCase.move(product, "list1", "list2")

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.saveProduct(product, "list2")
            repository.deleteProduct("p1", "list1")
        }
    }

    @Test
    fun `move fails and keeps the product at the origin when the destination save fails`() = runTest {
        coEvery { repository.saveProduct(product, "list2") } returns Result.failure(IllegalStateException("locked"))

        val result = useCase.move(product, "list1", "list2")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.deleteProduct(any(), any()) }
    }
}
