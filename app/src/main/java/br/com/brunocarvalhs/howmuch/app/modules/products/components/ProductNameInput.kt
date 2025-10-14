package br.com.brunocarvalhs.howmuch.app.modules.products.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryEditable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.constants.marketItems

@Composable
fun ProductNameInput(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        enabled = enabled,
        value = name,
        onValueChange = { onNameChange(it) },
        label = { Text(stringResource(R.string.product_name)) },
        singleLine = true,
        trailingIcon = {
            if (name.isNotEmpty() && enabled) {
                IconButton(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("clean_product_name"),
                    onClick = {
                        onNameChange(EMPTY_STRING)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clean)
                    )
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Campo de nome do produto com sugestões
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductNameInputWithSuggestions(
    name: String,
    onNameChange: (String) -> Unit,
    suggestions: List<String> = marketItems,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(name) {
        suggestions.filter { it.contains(name, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        ProductNameInput(
            name = name,
            onNameChange = onNameChange,
            modifier = modifier.menuAnchor(type = PrimaryEditable, enabled = true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filteredSuggestions.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onNameChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
