package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateAiSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(
        tier: AiConfigTier,
        model: String,
        prompt: String?,
        creativity: Float,
        customApiKey: String?
    ) {
        repository.updateAiSettings(tier, model, prompt, creativity, customApiKey)
    }
}
