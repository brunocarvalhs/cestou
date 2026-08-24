package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSearchUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSearchUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductSearchUseCase(repository)

    @Test
    fun `invoke delegates the query to the repository`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { repository.searchProducts("milk") } returns Result.success(listOf(product))

        val result = useCase("milk")

        assertEquals(listOf(product), result.getOrNull())
    }

    @Test
    fun `execute defaults to an empty query when missing`() = runTest {
        coEvery { repository.searchProducts("") } returns Result.success(emptyList())

        val result = useCase.execute(emptyMap(), mockk(relaxed = true), emptyMap())

        assertEquals(emptyList<Product>(), result.getOrNull())
    }
}
