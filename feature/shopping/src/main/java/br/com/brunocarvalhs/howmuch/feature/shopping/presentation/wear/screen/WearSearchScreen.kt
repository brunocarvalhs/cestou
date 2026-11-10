package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearSearchIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearSearchUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearSearchViewModel

/** Tags da tela de pesquisa, para os testes de UI (AGENTS §5: sem asserts de texto). */
internal const val TAG_SEARCH_EMPTY = "wear_search_empty"
internal const val TAG_SEARCH_ROW_PREFIX = "wear_search_row_"

/** Pesquisar (sdd-056, Q13): lista completa das listas, ativas e arquivadas, com busca por título. */
@Composable
internal fun WearSearchScreen(
    viewModel: WearSearchViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WearSearchContent(
        state = state,
        onQueryChange = { viewModel.onIntent(WearSearchIntent.QueryChanged(it)) },
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
private fun WearSearchContent(
    state: WearSearchUiState,
    onQueryChange: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val active = state.active.items
    val archived = state.archived.items

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_home_search))
                }
            }

            item {
                WearSearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                )
            }

            items(active.size, key = { active[it].id }) { index ->
                val shopping = active[index]
                WearListRow(
                    emoji = shopping.emoji,
                    title = shopping.title,
                    subtitle = shopping.budget?.let {
                        stringResource(R.string.shopping_wear_budget, it.formatPrice(LocalCurrency.current))
                    },
                    pinnedLabel = if (shopping.isFavorite) stringResource(R.string.shopping_wear_pinned) else null,
                    onClick = { onNavigateToDetail(shopping.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .testTag("$TAG_SEARCH_ROW_PREFIX${shopping.id}"),
                    transformation = SurfaceTransformation(spec)
                )
            }

            if (archived.isNotEmpty()) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec)
                    ) {
                        Text(stringResource(R.string.shopping_wear_archived_header))
                    }
                }
                items(archived.size, key = { archived[it].id }) { index ->
                    val shopping = archived[index]
                    WearListRow(
                        emoji = shopping.emoji,
                        title = shopping.title,
                        subtitle = stringResource(
                            R.string.shopping_wear_paid,
                            shopping.price.formatPrice(LocalCurrency.current)
                        ),
                        pinnedLabel = null,
                        onClick = { onNavigateToDetail(shopping.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .testTag("$TAG_SEARCH_ROW_PREFIX${shopping.id}"),
                        transformation = SurfaceTransformation(spec)
                    )
                }
            }

            if (!state.isLoading && active.isEmpty() && archived.isEmpty()) {
                item {
                    Text(
                        text = stringResource(emptyMessage(state.query)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(TAG_SEARCH_EMPTY)
                    )
                }
            }
        }
    }
}

/** Mensagem de lista vazia: sem busca, não há lista; com busca, nada bateu. */
private fun emptyMessage(query: String): Int =
    if (query.isBlank()) R.string.shopping_wear_no_lists else R.string.shopping_wear_search_empty
