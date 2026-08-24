package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductGetByBarcodeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductGetByBarcodeUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductGetByBarcodeUseCase(repository)

    @Test
    fun `invoke delegates to the repository and returns its result`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0, barcode = "789")
        coEvery { repository.getProductByBarcode("789") } returns Result.success(product)

        val result = useCase("789")

        assertEquals(product, result.getOrNull())
    }
}
