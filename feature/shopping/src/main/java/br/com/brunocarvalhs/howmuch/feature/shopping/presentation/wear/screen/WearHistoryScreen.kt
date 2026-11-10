package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearHistoryUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHistoryViewModel
import java.time.format.DateTimeFormatter

/** Histórico (sdd-056): quanto foi pago em cada mês, com a compra mais recente primeiro. */
@Composable
internal fun WearHistoryScreen(viewModel: WearHistoryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WearHistoryContent(state = state)
}

@Composable
private fun WearHistoryContent(state: WearHistoryUiState) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val months = state.months.items

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
                    Text(stringResource(R.string.shopping_wear_home_history))
                }
            }

            if (!state.isLoading && months.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.shopping_wear_history_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(months.size, key = { months[it].key.toString() }) { index ->
                HistoryMonthRow(month = months[index])
            }
        }
    }
}

@Composable
private fun HistoryMonthRow(month: HistoryMonth) {
    val locale = LocalConfiguration.current.locales[0]
    val label = remember(month.key, locale) {
        month.key.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = month.total.formatPrice(LocalCurrency.current),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
