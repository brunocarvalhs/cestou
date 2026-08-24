package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.GetProductSuggestionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProductSuggestionsUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = GetProductSuggestionsUseCase(repository)

    @Test
    fun `invoke returns the suggestions flow from the repository`() = runTest {
        val suggestion = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        every { repository.getSuggestions("list1") } returns flowOf(listOf(suggestion))

        assertEquals(listOf(suggestion), useCase("list1").first())
    }
}
