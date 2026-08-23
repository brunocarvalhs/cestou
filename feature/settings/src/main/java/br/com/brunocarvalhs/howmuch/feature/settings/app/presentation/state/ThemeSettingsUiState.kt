package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode

@Stable
internal data class ThemeSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
