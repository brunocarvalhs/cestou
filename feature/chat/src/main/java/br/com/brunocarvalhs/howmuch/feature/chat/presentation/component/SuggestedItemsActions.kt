package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import br.com.brunocarvalhs.howmuch.feature.chat.R

/** Ações sobre os itens que a IA sugeriu: "+" em cada um e "Adicionar todos" quando há mais de um. */
@Composable
internal fun SuggestedItemsActions(
    items: List<String>,
    onAddItem: (String) -> Unit,
    onAddAll: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onAddItem(item) },
                    modifier = Modifier.testTag("ai_suggested_item_add_$index"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.ai_chat_add_item_content_description, item),
                    )
                }
            }
        }
        if (items.size > 1) {
            TextButton(
                onClick = { onAddAll(items) },
                modifier = Modifier.testTag("ai_suggested_items_add_all"),
            ) {
                Text(text = stringResource(R.string.ai_chat_add_all_items))
            }
        }
    }
}
