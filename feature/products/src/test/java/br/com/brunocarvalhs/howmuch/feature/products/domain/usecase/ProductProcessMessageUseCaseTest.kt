package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductProcessMessageUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductProcessMessageUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductProcessMessageUseCase(repository)

    @Test
    fun `invoke always succeeds with an empty list`() = runTest {
        val result = useCase("any message")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
}
