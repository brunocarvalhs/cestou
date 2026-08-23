package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.ShoppingSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.ShoppingSettingsUiState

@Composable
internal fun ShoppingSettingsScreen(
    state: ShoppingSettingsUiState,
    intent: ShoppingSettingsIntent
) {
    var sortingMode by remember(state.sortingMode) {
        mutableStateOf(state.sortingMode)
    }
    var remindersEnabled by remember(state.remindersEnabled) {
        mutableStateOf(state.remindersEnabled)
    }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_shopping),
                onBack = { 
                    intent.onUpdateShoppingPreferences(
                        state.defaultListId,
                        sortingMode,
                        remindersEnabled
                    )
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

            Text(
                text = stringResource(R.string.settings_shopping_default_sorting),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = sortingMode == "CATEGORY",
                    onClick = { sortingMode = "CATEGORY" }
                )
                Text(
                    text = stringResource(R.string.settings_shopping_sort_category),
                    modifier = Modifier.padding(start = 8.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                RadioButton(
                    selected = sortingMode == "NAME",
                    onClick = { sortingMode = "NAME" }
                )
                Text(
                    text = stringResource(R.string.settings_shopping_sort_name),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_shopping_reminders),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_shopping_reminders_desc),
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
                    intent.onUpdateShoppingPreferences(
                        state.defaultListId,
                        sortingMode,
                        remindersEnabled
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_ai_button_save))
            }
        }
    }
}

@Preview(showBackground = true, name = "Ordenar por categoria")
@Composable
private fun ShoppingSettingsScreenCategoryPreview() {
    MaterialTheme {
        ShoppingSettingsScreen(
            state = ShoppingSettingsUiState(sortingMode = "CATEGORY", remindersEnabled = false),
            intent = ShoppingSettingsIntent()
        )
    }
}

@Preview(showBackground = true, name = "Ordenar por nome, lembretes ativos")
@Composable
private fun ShoppingSettingsScreenNamePreview() {
    MaterialTheme {
        ShoppingSettingsScreen(
            state = ShoppingSettingsUiState(sortingMode = "NAME", remindersEnabled = true),
            intent = ShoppingSettingsIntent()
        )
    }
}
