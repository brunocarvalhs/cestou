package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.PriceComparisonUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceComparisonUseCaseTest {

    private val useCase = PriceComparisonUseCase()

    @Test
    fun `execute returns three simulated price comparisons for a product`() = runTest {
        val result = useCase.execute(mapOf("productName" to "Milk"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun `execute fails when productName is missing`() = runTest {
        val result = useCase.execute(emptyMap(), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }
}
