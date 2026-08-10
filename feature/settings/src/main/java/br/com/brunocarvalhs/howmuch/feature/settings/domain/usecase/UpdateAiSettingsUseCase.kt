package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateAiSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(model: String, prompt: String?, creativity: Float) {
        repository.updateAiSettings(model, prompt, creativity)
    }
}
