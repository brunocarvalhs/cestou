package br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateAiSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(model: String, prompt: String?, creativity: Float) {
        repository.updateAiSettings(model, prompt, creativity)
    }
}
