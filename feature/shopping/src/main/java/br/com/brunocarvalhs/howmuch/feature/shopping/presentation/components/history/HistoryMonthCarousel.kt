package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import java.time.YearMonth

@Composable
internal fun HistoryMonthCarousel(
    months: StableList<HistoryMonth>,
    selectedMonth: YearMonth?,
    onMonthSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratios = remember(months) { trendRatios(months.map { it.total }) }
    val listState = rememberLazyListState()
    // Keyed on the selection only: a new emission of the same months must not yank the carousel
    // back while the user is scrolling it.
    val currentMonths by rememberUpdatedState(months)
    LaunchedEffect(selectedMonth) {
        val index = currentMonths.indexOfFirst { it.key == selectedMonth }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(months.items, key = { _, month -> month.key.toString() }, contentType = { _, _ -> "month" }) {
                index, month ->
            HistoryMonthCard(
                month = month,
                ratio = ratios[index],
                previousRatio = ratios.getOrNull(index - 1),
                nextRatio = ratios.getOrNull(index + 1),
                selected = month.key == selectedMonth,
                onClick = { onMonthSelect(month.key) }
            )
        }
    }
}
