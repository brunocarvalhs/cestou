package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ShoppingSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ShoppingSettingsUiState

@Composable
internal fun ShoppingSettingsScreen(
    state: ShoppingSettingsUiState,
    intent: ShoppingSettingsIntent
) {
    var sortingMode by remember(state.sortingMode) { mutableStateOf(state.sortingMode) }
    var remindersEnabled by remember(state.remindersEnabled) { mutableStateOf(state.remindersEnabled) }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_shopping),
                onBack = { 
                    intent.onUpdateShoppingPreferences(state.defaultListId, sortingMode, remindersEnabled)
                    intent.onBack() 
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.settings_shopping_preferences),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(stringResource(R.string.settings_shopping_default_sorting), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = sortingMode == "CATEGORY",
                    onClick = { sortingMode = "CATEGORY" }
                )
                Text(stringResource(R.string.settings_shopping_sort_category), modifier = Modifier.padding(start = 8.dp))
                
                Spacer(modifier = Modifier.width(16.dp))
                
                RadioButton(
                    selected = sortingMode == "NAME",
                    onClick = { sortingMode = "NAME" }
                )
                Text(stringResource(R.string.settings_shopping_sort_name), modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_shopping_reminders), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_shopping_reminders_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    intent.onUpdateShoppingPreferences(state.defaultListId, sortingMode, remindersEnabled)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_ai_button_save))
            }
        }
    }
}
