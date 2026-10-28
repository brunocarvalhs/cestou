package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel

@Stable
internal data class AiSettingsUiState(
    val aiModel: String = "",
    val customPrompt: String? = null,
    val creativityLevel: Float = 0.7f,
    val availableModels: List<AiModel> = emptyList(),
    val isLoadingModels: Boolean = false
)
