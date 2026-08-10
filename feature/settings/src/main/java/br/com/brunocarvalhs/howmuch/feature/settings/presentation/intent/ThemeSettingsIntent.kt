package br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode

internal data class ThemeSettingsIntent(
    val onUpdateTheme: (ThemeMode) -> Unit = {},
    val onBack: () -> Unit = {}
)
