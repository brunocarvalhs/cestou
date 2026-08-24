package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateAiSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateAiSettingsUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateAiSettingsUseCase(repository)

    @Test
    fun `invoke forwards model, prompt and creativity to the repository`() = runTest {
        coEvery { repository.updateAiSettings("gemini", "be concise", 0.5f) } returns Unit

        useCase("gemini", "be concise", 0.5f)

        coVerify { repository.updateAiSettings("gemini", "be concise", 0.5f) }
    }
}
