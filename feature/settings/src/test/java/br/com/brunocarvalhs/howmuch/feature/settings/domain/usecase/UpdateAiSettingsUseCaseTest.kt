package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val TEST_TEMPERATURE = 0.5f

class UpdateAiSettingsUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateAiSettingsUseCase(repository)

    @Test
    fun `invoke forwards model, prompt and creativity to the repository`() = runTest {
        coEvery { repository.updateAiSettings("gemini", "be concise", TEST_TEMPERATURE) } returns Unit

        useCase("gemini", "be concise", TEST_TEMPERATURE)

        coVerify { repository.updateAiSettings("gemini", "be concise", TEST_TEMPERATURE) }
    }
}
