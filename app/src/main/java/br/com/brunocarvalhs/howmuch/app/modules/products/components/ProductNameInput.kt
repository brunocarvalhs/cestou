package br.com.brunocarvalhs.howmuch.app.modules.products.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING

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
