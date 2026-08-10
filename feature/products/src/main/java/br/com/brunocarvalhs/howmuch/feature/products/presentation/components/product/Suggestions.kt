package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R

@Composable
fun Suggestions(
    items: List<String> = emptyList(),
    isLoading: Boolean = false,
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() && !isLoading) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Text(
            text = if (isLoading) {
                stringResource(R.string.ai_thinking)
            } else {
                stringResource(R.string.ai_suggestions_title)
            },
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(
            Modifier.height(12.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    SuggestionChip(
                        onClick = { onItemClick(item) },
                        label = {
                            Text(item)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionsPreview() {
    MaterialTheme {
        Suggestions(
            items = listOf("Comprar leite", "Verificar ofertas de carne", "Adicionar frutas"),
            isLoading = false,
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionsLoadingPreview() {
    MaterialTheme {
        Suggestions(
            items = emptyList(),
            isLoading = true,
            onItemClick = {}
        )
    }
}
