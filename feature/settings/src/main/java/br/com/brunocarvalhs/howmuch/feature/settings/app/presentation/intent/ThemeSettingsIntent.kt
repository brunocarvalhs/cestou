package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode

internal data class ThemeSettingsIntent(
    val onUpdateTheme: (ThemeMode) -> Unit = {},
    val onBack: () -> Unit = {}
)
