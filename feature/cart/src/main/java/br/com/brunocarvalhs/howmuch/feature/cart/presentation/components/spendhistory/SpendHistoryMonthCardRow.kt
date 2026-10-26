package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.MonthSpendSummary

private val CARD_MIN_WIDTH = 88.dp
private val MARKER_ZONE_HEIGHT = 20.dp
private val MARKER_SIZE = 12.dp
private val SEGMENT_WIDTH = 16.dp
private val LINE_THICKNESS = 2.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)

/**
 * Month-card row with a trend line connecting each card's marker — the Magalu Cashback /
 * Nubank-style statement bruno referenced. Horizontally scrollable so it also works once the
 * real data spec's "6 months, 4 visible" range lands; with fewer months (a brand-new account)
 * it simply renders fewer cards and fewer line segments instead of padding with empty ones.
 *
 * The current month is distinguishable by more than color alone (per the accessibility note on
 * not repeating the `item-add-authorship` selected-chip contrast finding): it gets an outlined
 * border, an open/unfilled ring marker instead of a filled dot, bold text, *and* a visible
 * "Parcial" label — so the distinction survives grayscale and screen readers, not just color.
 */
@Composable
internal fun SpendHistoryMonthCardRow(
    months: StableList<MonthSpendSummary>,
    selectedMonthId: String?,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = rememberCurrencyFormatter()
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        months.forEachIndexed { index, month ->
            if (index > 0) {
                TrendLineSegment(isAccent = month.isCurrent)
            }
            MonthCard(
                month = month,
                isSelected = month.id == selectedMonthId,
                totalLabel = currencyFormatter.format(month.total),
                onClick = { onMonthSelected(month.id) }
            )
        }
    }
}

@Composable
private fun TrendLineSegment(isAccent: Boolean) {
    val color = if (isAccent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .width(SEGMENT_WIDTH)
            .height(MARKER_ZONE_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(SEGMENT_WIDTH)
                .height(LINE_THICKNESS)
                .background(color)
        )
    }
}

@Composable
private fun MonthCard(
    month: MonthSpendSummary,
    isSelected: Boolean,
    totalLabel: String,
    onClick: () -> Unit
) {
    val isCurrent = month.isCurrent
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val emphasisColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderModifier = if (isCurrent) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CARD_SHAPE)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .widthIn(min = CARD_MIN_WIDTH)
            .clip(CARD_SHAPE)
            .then(borderModifier)
            .background(containerColor)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .testTag("spend_history_month_card_${month.id}")
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthMarker(isCurrent = isCurrent, tint = emphasisColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = month.monthLabel,
            style = MaterialTheme.typography.labelLarge,
            color = emphasisColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
        )
        if (isCurrent) {
            Text(
                text = stringResource(R.string.spend_history_current_month_badge),
                style = MaterialTheme.typography.labelSmall,
                color = emphasisColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = totalLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = emphasisColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** Filled dot for a closed month, open ring for the in-progress one — shape, not just color. */
@Composable
private fun MonthMarker(isCurrent: Boolean, tint: Color) {
    val markerModifier = if (isCurrent) {
        Modifier.border(2.dp, tint, CircleShape)
    } else {
        Modifier
            .clip(CircleShape)
            .background(tint)
    }
    Box(modifier = Modifier.size(MARKER_SIZE).then(markerModifier))
}

private val previewMonths = StableList(
    listOf(
        MonthSpendSummary(id = "2026-03", monthLabel = "Mar", fullLabel = "Março", total = 26.58),
        MonthSpendSummary(id = "2026-04", monthLabel = "Abr", fullLabel = "Abril", total = 70.63),
        MonthSpendSummary(id = "2026-05", monthLabel = "Mai", fullLabel = "Maio", total = 76.12),
        MonthSpendSummary(
            id = "2026-06",
            monthLabel = "Jun",
            fullLabel = "Junho",
            total = 42.90,
            isCurrent = true
        )
    )
)

@Preview(showBackground = true)
@Composable
private fun SpendHistoryMonthCardRowPreview() {
    CestouTheme {
        SpendHistoryMonthCardRow(
            months = previewMonths,
            selectedMonthId = "2026-06",
            onMonthSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "Primeiro mês de uso")
@Composable
private fun SpendHistoryMonthCardRowFirstUsePreview() {
    CestouTheme {
        SpendHistoryMonthCardRow(
            months = StableList(listOf(previewMonths.last())),
            selectedMonthId = "2026-06",
            onMonthSelected = {}
        )
    }
}
