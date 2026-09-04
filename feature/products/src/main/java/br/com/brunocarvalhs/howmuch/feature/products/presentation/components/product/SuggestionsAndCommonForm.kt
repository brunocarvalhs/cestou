package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import br.com.brunocarvalhs.howmuch.feature.products.presentation.event.ProductSuggestionEvent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductSuggestionIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductSuggestionUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * [Options.SUGGESTIONS]'s content.
 *
 * Used to also host a "Common" toggle (`CommonProductForm`/`CommonProductViewModel`) alongside
 * suggestions. That data source moved to [Options.QUICK_ADD] instead — common items now have one
 * path, not two (`.specs/features/item-add-authorship/design.md`, "Quick Add's common-product
 * chips"). This composable stays as a thin wrapper (rather than being inlined at the call site) so
 * `ProductScreen`'s per-tab composable shape doesn't change.
 */
@Composable
internal fun SuggestionsAndCommonForm(
    modifier: Modifier = Modifier,
    suggestionUiState: ProductSuggestionUiState,
    suggestionIntent: ProductSuggestionIntent,
    suggestionEvents: Flow<ProductSuggestionEvent>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        ProductSuggestionForm(
            uiState = suggestionUiState,
            events = suggestionEvents,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            intent = suggestionIntent
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SuggestionsAndCommonFormPreview() {
    SuggestionsAndCommonForm(
        suggestionUiState = ProductSuggestionUiState(),
        suggestionIntent = ProductSuggestionIntent(),
        suggestionEvents = emptyFlow()
    )
}
