package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.AiSettingsUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiSettingsScreen(
    state: AiSettingsUiState,
    intent: AiSettingsIntent
) {
    var model by remember(state.aiModel) {
        mutableStateOf(state.aiModel)
    }
    var prompt by remember(state.customPrompt) {
        mutableStateOf(state.customPrompt ?: "")
    }
    var creativity by remember(state.creativityLevel) {
        mutableFloatStateOf(state.creativityLevel)
    }

    var showModelDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_ai),
                onBack = { 
                    intent.onUpdateAiSettings(model, if (prompt.isEmpty()) null else prompt, creativity)
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
                text = stringResource(R.string.settings_ai_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = showModelDropdown,
                    onExpandedChange = { showModelDropdown = !showModelDropdown }
                ) {
                    OutlinedTextField(
                        value = AiModel.freeModels.find { it.id == model }?.name ?: model,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_ai_label_model)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown)
                        },
                        modifier = Modifier
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false }
                    ) {
                        AiModel.freeModels.forEach { aiModel ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(aiModel.name)
                                        Text(aiModel.provider, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    model = aiModel.id
                                    showModelDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.settings_ai_label_custom_prompt)) },
                placeholder = { Text(stringResource(R.string.settings_ai_placeholder_custom_prompt)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                minLines = 3
            )

            Text(
                text = stringResource(R.string.settings_ai_label_creativity, creativity),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = creativity,
                onValueChange = { creativity = it },
                valueRange = 0f..1f,
                steps = 10,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    intent.onUpdateAiSettings(model, if (prompt.isEmpty()) null else prompt, creativity)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.settings_ai_button_save))
            }
        }
    }
}

@Preview(showBackground = true, name = "Padrão")
@Composable
private fun AiSettingsScreenDefaultPreview() {
    MaterialTheme {
        AiSettingsScreen(
            state = AiSettingsUiState(),
            intent = AiSettingsIntent()
        )
    }
}

@Preview(showBackground = true, name = "Personalizado")
@Composable
private fun AiSettingsScreenCustomizedPreview() {
    MaterialTheme {
        AiSettingsScreen(
            state = AiSettingsUiState(
                aiModel = AiModel.freeModels.first().id,
                customPrompt = "Sempre responda de forma breve e direta.",
                creativityLevel = 0.9f
            ),
            intent = AiSettingsIntent()
        )
    }
}
