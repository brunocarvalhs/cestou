package br.com.brunocarvalhs.howmuch.feature.profile.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.SettingsSectionsList
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState

@Composable
internal fun ProfileSettingsSection(
    settingsState: SettingsUiState,
    settingsIntent: SettingsIntent,
    modifier: Modifier = Modifier
) {
    SettingsSectionsList(
        sections = settingsState.sections,
        expandedSectionIds = settingsState.expandedSectionIds,
        intent = settingsIntent,
        modifier = modifier
    )
}
