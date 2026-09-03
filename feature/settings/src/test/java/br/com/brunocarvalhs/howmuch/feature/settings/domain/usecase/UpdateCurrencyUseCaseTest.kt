package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateCurrencyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCurrencyUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateCurrencyUseCase(repository)

    @Test
    fun `invoke updates the currency through the repository`() = runTest {
        coEvery { repository.updateCurrency("USD") } returns Unit

        val result = useCase("USD")

        assertTrue(result.isSuccess)
        coVerify { repository.updateCurrency("USD") }
    }

    @Test
    fun `invoke fails when the repository throws`() = runTest {
        coEvery { repository.updateCurrency("USD") } throws IllegalStateException("boom")

        assertTrue(useCase("USD").isFailure)
    }
}
