package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.shopping.R

@Composable
internal fun ShoppingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.shopping_management_search_placeholder),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(
                                br.com.brunocarvalhs.howmuch.core.ui.R.string.action_close
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onOpenAiChat) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(
                            br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_ai_icon
                        ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ShoppingSearchBarEmptyPreview() {
    CestouTheme {
        ShoppingSearchBar(
            query = "",
            onQueryChange = {},
            onOpenAiChat = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ShoppingSearchBarWithTextPreview() {
    CestouTheme {
        ShoppingSearchBar(
            query = "Churrasco",
            onQueryChange = {},
            onOpenAiChat = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
