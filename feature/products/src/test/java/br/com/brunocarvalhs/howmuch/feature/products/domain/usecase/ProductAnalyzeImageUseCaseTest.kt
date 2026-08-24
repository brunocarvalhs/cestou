package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import android.graphics.Bitmap
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductAnalyzeImageUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductAnalyzeImageUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductAnalyzeImageUseCase(repository)

    @Test
    fun `invoke delegates to the repository and returns its result`() = runTest {
        val bitmap = mockk<Bitmap>()
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { repository.analyzeImage(bitmap) } returns Result.success(listOf(product))

        val result = useCase(bitmap)

        assertEquals(listOf(product), result.getOrNull())
    }
}
