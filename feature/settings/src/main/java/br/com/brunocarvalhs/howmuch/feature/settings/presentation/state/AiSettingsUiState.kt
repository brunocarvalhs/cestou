package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class AiSettingsUiState(
    val aiModel: String = "",
    val customPrompt: String? = null,
    val creativityLevel: Float = 0.7f
)
