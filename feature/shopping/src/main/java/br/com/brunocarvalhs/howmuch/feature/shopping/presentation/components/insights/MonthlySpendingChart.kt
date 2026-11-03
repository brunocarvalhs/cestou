package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.MonthSpending
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.fullLabel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.shortLabel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.trendRatios
import java.time.YearMonth

private const val BAR_WIDTH_FRACTION = 0.6f
private val BarAreaHeight = 120.dp
private val MinBarHeight = 4.dp
private val MinTouchTarget = 48.dp
private val BarCorner = 8.dp

/**
 * Bar chart of the total spent per month. Each bar is drawn on a [Canvas] sized by its share of the
 * largest total; the column around it is the touch target and carries the semantics, so no manual
 * hit-test is needed. The amount is written out in the description, never only encoded as height
 * or colour.
 */
@Composable
internal fun MonthlySpendingChart(
    months: StableList<MonthSpending>,
    selectedMonth: YearMonth?,
    onMonthSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratios = remember(months) { trendRatios(months.map { it.total }) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEachIndexed { index, month ->
            MonthBar(
                month = month,
                ratio = ratios[index],
                selected = month.key == selectedMonth,
                onClick = { onMonthSelect(month.key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthBar(
    month: MonthSpending,
    ratio: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val total = month.total.formatPrice(LocalCurrency.current)
    val description = stringResource(
        if (selected) R.string.history_month_description_selected else R.string.history_month_description,
        month.key.fullLabel(),
        total
    )
    Column(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .testTag("insights_month_${month.key}")
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                contentDescription = description
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val barColor = if (selected) colors.primary else colors.primaryContainer
        Canvas(modifier = Modifier.height(BarAreaHeight).fillMaxWidth().clearAndSetSemantics { }) {
            val barWidth = size.width * BAR_WIDTH_FRACTION
            val barHeight = (size.height * ratio).coerceAtLeast(MinBarHeight.toPx())
            val corner = CornerRadius(BarCorner.toPx())
            drawRoundRect(
                color = barColor,
                topLeft = Offset((size.width - barWidth) / 2, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
        }
        Text(
            text = month.key.shortLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).clearAndSetSemantics { }
        )
    }
}
