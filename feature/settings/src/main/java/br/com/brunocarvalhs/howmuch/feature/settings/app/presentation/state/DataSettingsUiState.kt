package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class DataSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
