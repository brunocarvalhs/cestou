package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Chips de prompts rápidos acima da barra de digitação; tocar num deles envia o texto como mensagem. */
@Composable
internal fun AiSuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.testTag("ai_suggestion_chips"),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(suggestions) { index, suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(text = suggestion) },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .testTag("ai_suggestion_chip_$index"),
            )
        }
    }
}
