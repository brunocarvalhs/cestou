package br.com.brunocarvalhs.howmuch.app.modules.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick

@Composable
fun HistoryFilter(
    selectedFilter: HistoryFilterType,
    onFilterSelected: (HistoryFilterType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(HistoryFilterType.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = {
                    onFilterSelected(filter)
                    trackClick(
                        viewId = "filter_${filter.name.lowercase()}",
                        viewName = "History Filter: ${filter.name}",
                        screenName = "HistoryScreen"
                    )
                },
                label = { Text(stringResource(filter.displayName)) }
            )
        }
    }
}
