package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonProductRemoveUseCaseTest {

    private val repository = mockk<CommonProductRepository>(relaxed = true)
    private val useCase = CommonProductRemoveUseCase(repository)

    @Test
    fun `invoke removes the product by id`() = runTest {
        coEvery { repository.remove("common-1") } returns Result.success(Unit)

        val result = useCase("common-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.remove("common-1") }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val failure = IllegalStateException("not found")
        coEvery { repository.remove("missing") } returns Result.failure(failure)

        val result = useCase("missing")

        assertTrue(result.isFailure)
        assertEquals(failure, result.exceptionOrNull())
    }
}
