package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.GetQuestionSuggestionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetQuestionSuggestionsUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = GetQuestionSuggestionsUseCase(repository)

    @Test
    fun `invoke returns the question suggestions flow from the repository`() = runTest {
        every { repository.getQuestionSuggestions("list1") } returns flowOf(listOf("Qual o orçamento?"))

        assertEquals(listOf("Qual o orçamento?"), useCase("list1").first())
    }
}
