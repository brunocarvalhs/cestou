package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ThemeSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ThemeSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSettingsScreen(
    state: ThemeSettingsUiState,
    intent: ThemeSettingsIntent
) {
    var showThemeBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_item_theme),
                onBack = { intent.onBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ListItem(
                modifier = Modifier.clickable { showThemeBottomSheet = true },
                headlineContent = { Text(stringResource(R.string.settings_item_theme)) },
                supportingContent = {
                    val currentThemeLabel = when (state.themeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                    Text(currentThemeLabel)
                }
            )
        }
    }

    if (showThemeBottomSheet) {
        ThemeBottomSheet(
            onDismiss = { showThemeBottomSheet = false },
            onSelect = { theme ->
                intent.onUpdateTheme(theme)
                showThemeBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_select),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            ThemeOption(stringResource(R.string.settings_theme_system), ThemeMode.SYSTEM, onSelect)
            ThemeOption(stringResource(R.string.settings_theme_light), ThemeMode.LIGHT, onSelect)
            ThemeOption(stringResource(R.string.settings_theme_dark), ThemeMode.DARK, onSelect)
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    mode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
    }
}

@Preview(showBackground = true, name = "Sistema")
@Composable
private fun ThemeSettingsScreenSystemPreview() {
    MaterialTheme {
        ThemeSettingsScreen(
            state = ThemeSettingsUiState(themeMode = ThemeMode.SYSTEM),
            intent = ThemeSettingsIntent()
        )
    }
}

@Preview(showBackground = true, name = "Claro")
@Composable
private fun ThemeSettingsScreenLightPreview() {
    MaterialTheme {
        ThemeSettingsScreen(
            state = ThemeSettingsUiState(themeMode = ThemeMode.LIGHT),
            intent = ThemeSettingsIntent()
        )
    }
}

@Preview(showBackground = true, name = "Escuro")
@Composable
private fun ThemeSettingsScreenDarkPreview() {
    MaterialTheme {
        ThemeSettingsScreen(
            state = ThemeSettingsUiState(themeMode = ThemeMode.DARK),
            intent = ThemeSettingsIntent()
        )
    }
}
