package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode

internal data class ThemeSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
