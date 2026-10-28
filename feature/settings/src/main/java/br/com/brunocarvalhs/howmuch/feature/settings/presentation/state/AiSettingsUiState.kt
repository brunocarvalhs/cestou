package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus

@Stable
internal data class AiSettingsUiState(
    val aiConfigTier: AiConfigTier = AiConfigTier.DEFAULT,
    val aiModel: String = "",
    val customPrompt: String? = null,
    val customApiKey: String = "",
    val creativityLevel: Float = 0.7f,
    val availableModels: List<AiModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE
)
