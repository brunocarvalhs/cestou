package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonProductAddUseCaseTest {

    private val repository = mockk<CommonProductRepository>(relaxed = true)
    private val useCase = CommonProductAddUseCase(repository)

    @Test
    fun `invoke adds product with default category and unit`() = runTest {
        coEvery { repository.add(any()) } returns Result.success(Unit)

        val result = useCase(name = "Arroz")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            repository.add(
                match {
                    it.name == "Arroz" && it.category == "Outros" && it.unit == "un"
                }
            )
        }
    }

    @Test
    fun `invoke forwards custom category and unit`() = runTest {
        coEvery { repository.add(any()) } returns Result.success(Unit)

        useCase(name = "Leite", category = "Laticinios", unit = "L")

        coVerify(exactly = 1) {
            repository.add(
                match<CommonProduct> {
                    it.name == "Leite" && it.category == "Laticinios" && it.unit == "L"
                }
            )
        }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val failure = IllegalStateException("boom")
        coEvery { repository.add(any()) } returns Result.failure(failure)

        val result = useCase(name = "Feijao")

        assertTrue(result.isFailure)
        assertEquals(failure, result.exceptionOrNull())
    }
}
