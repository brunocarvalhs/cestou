package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.event.ProductSuggestionEvent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.CommonProductIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductSuggestionIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.CommonProductUiState
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductSuggestionUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private enum class SuggestionsAndCommonMode { SUGGESTIONS, COMMON }

@Composable
internal fun SuggestionsAndCommonForm(
    modifier: Modifier = Modifier,
    suggestionUiState: ProductSuggestionUiState,
    suggestionIntent: ProductSuggestionIntent,
    suggestionEvents: Flow<ProductSuggestionEvent>,
    commonUiState: CommonProductUiState,
    commonIntent: CommonProductIntent,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {}
) {
    var mode by rememberSaveable { mutableStateOf(SuggestionsAndCommonMode.SUGGESTIONS) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 16.dp, 16.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == SuggestionsAndCommonMode.SUGGESTIONS,
                onClick = { mode = SuggestionsAndCommonMode.SUGGESTIONS },
                label = { Text(stringResource(R.string.product_option_suggestions)) },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }
            )
            FilterChip(
                selected = mode == SuggestionsAndCommonMode.COMMON,
                onClick = { mode = SuggestionsAndCommonMode.COMMON },
                label = { Text(stringResource(R.string.product_option_common)) },
                leadingIcon = { Icon(Icons.Default.ShoppingBasket, null) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                SuggestionsAndCommonMode.SUGGESTIONS -> ProductSuggestionForm(
                    uiState = suggestionUiState,
                    events = suggestionEvents,
                    snackbarHostState = snackbarHostState,
                    onBack = onBack,
                    intent = suggestionIntent
                )

                SuggestionsAndCommonMode.COMMON -> CommonProductForm(
                    uiState = commonUiState,
                    intent = commonIntent,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SuggestionsAndCommonFormPreview() {
    SuggestionsAndCommonForm(
        suggestionUiState = ProductSuggestionUiState(),
        suggestionIntent = ProductSuggestionIntent(),
        suggestionEvents = emptyFlow(),
        commonUiState = CommonProductUiState(),
        commonIntent = CommonProductIntent()
    )
}
