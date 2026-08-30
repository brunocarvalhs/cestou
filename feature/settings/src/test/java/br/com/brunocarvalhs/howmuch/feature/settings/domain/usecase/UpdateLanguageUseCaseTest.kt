package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateLanguageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLanguageUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateLanguageUseCase(repository)

    @Test
    fun `invoke updates the language through the repository`() = runTest {
        coEvery { repository.updateLanguage("en") } returns Unit

        val result = useCase("en")

        assertTrue(result.isSuccess)
        coVerify { repository.updateLanguage("en") }
    }

    @Test
    fun `invoke fails when the repository throws`() = runTest {
        coEvery { repository.updateLanguage("en") } throws IllegalStateException("boom")

        assertTrue(useCase("en").isFailure)
    }
}
