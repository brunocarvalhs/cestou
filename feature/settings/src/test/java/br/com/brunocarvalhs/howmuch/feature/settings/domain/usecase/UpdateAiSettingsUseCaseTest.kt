package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
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
    fun `invoke forwards tier, model, prompt, creativity and customApiKey to the repository`() =
        runTest {
            coEvery {
                repository.updateAiSettings(
                    AiConfigTier.DEFAULT,
                    "gemini",
                    "be concise",
                    TEST_TEMPERATURE,
                    null
                )
            } returns Unit

            useCase(
                tier = AiConfigTier.DEFAULT,
                model = "gemini",
                prompt = "be concise",
                creativity = TEST_TEMPERATURE,
                customApiKey = null
            )

            coVerify {
                repository.updateAiSettings(
                    AiConfigTier.DEFAULT,
                    "gemini",
                    "be concise",
                    TEST_TEMPERATURE,
                    null
                )
            }
        }
}
