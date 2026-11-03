package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter

private fun NotificationFilter.tag() = "notifications_filter_${name.lowercase()}"

@Composable
internal fun NotificationFilterChips(
    selected: NotificationFilter,
    onSelect: (NotificationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NotificationFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                modifier = Modifier.testTag(filter.tag()),
                label = {
                    Text(
                        text = stringResource(
                            when (filter) {
                                NotificationFilter.ALL -> R.string.notifications_filter_all
                                NotificationFilter.LISTS -> R.string.notifications_filter_lists
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
