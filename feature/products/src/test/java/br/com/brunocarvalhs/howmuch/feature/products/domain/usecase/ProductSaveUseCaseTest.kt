package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSaveUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductSaveUseCase(repository)

    @Test
    fun `invoke by name and quantity builds and saves a new product`() = runTest {
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase(name = "Milk", quantity = 2.0, shoppingId = "list1")

        assertTrue(result.isSuccess)
        coVerify { repository.saveProduct(match { it.name == "Milk" && it.quantity == 2.0 }, "list1") }
    }

    @Test
    fun `invoke by product forwards straight to the repository`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { repository.saveProduct(product, "list1") } returns Result.success(Unit)

        val result = useCase(product, "list1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `execute reads name, quantity and price from arguments`() = runTest {
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase.execute(
            mapOf("name" to "Milk", "quantity" to 2.0, "price" to 5.0, "shoppingId" to "list1"),
            mockk(relaxed = true),
            emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.saveProduct(match { it.name == "Milk" && it.quantity == 2.0 && it.price == 5.0 }, "list1")
        }
    }

    @Test
    fun `execute defaults quantity and price when absent`() = runTest {
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        useCase.execute(mapOf("name" to "Milk", "shoppingId" to "list1"), mockk(relaxed = true), emptyMap())

        coVerify {
            repository.saveProduct(match { it.quantity == 1.0 && it.price == 0.0 }, "list1")
        }
    }

    @Test
    fun `execute fails when name is missing`() = runTest {
        val result = useCase.execute(mapOf("shoppingId" to "list1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when shoppingId is missing from both arguments and metadata`() = runTest {
        val result = useCase.execute(mapOf("name" to "Milk"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }
}
