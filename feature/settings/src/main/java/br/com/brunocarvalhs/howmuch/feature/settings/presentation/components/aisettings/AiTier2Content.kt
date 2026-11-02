package br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.aisettings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.feature.settings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiTier2Content(
    selectedModel: String,
    availableModels: List<AiModel>,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(8.dp))
    Box(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = showDropdown,
            onExpandedChange = { showDropdown = !showDropdown }
        ) {
            OutlinedTextField(
                value = availableModels.find { it.id == selectedModel }?.name ?: selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_ai_label_model)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                availableModels.forEach { aiModel ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(aiModel.name)
                                Text(aiModel.vendor, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        onClick = {
                            onModelSelected(aiModel.id)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}
