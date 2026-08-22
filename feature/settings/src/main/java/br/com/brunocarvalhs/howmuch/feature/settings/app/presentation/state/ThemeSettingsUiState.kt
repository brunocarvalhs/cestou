package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode

internal data class ThemeSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
