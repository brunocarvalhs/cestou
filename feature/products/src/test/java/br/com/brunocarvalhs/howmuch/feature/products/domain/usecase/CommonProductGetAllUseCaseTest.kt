package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CommonProductGetAllUseCaseTest {

    private val repository = mockk<CommonProductRepository>(relaxed = true)
    private val useCase = CommonProductGetAllUseCase(repository)

    @Test
    fun `invoke seeds defaults before returning the current list`() = runTest {
        val items = listOf(CommonProduct(id = "1", name = "Arroz"))
        coEvery { repository.seedDefaultsIfEmpty() } returns Result.success(Unit)
        coEvery { repository.getAll() } returns flowOf(items)

        val result = useCase().first()

        assertEquals(items, result)
        coVerifyOrder {
            repository.seedDefaultsIfEmpty()
            repository.getAll()
        }
    }

    @Test
    fun `invoke still reads from repository when seeding fails`() = runTest {
        coEvery { repository.seedDefaultsIfEmpty() } returns Result.failure(IllegalStateException("seed failed"))
        coEvery { repository.getAll() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(emptyList<CommonProduct>(), result)
        coVerify(exactly = 1) { repository.getAll() }
    }
}
