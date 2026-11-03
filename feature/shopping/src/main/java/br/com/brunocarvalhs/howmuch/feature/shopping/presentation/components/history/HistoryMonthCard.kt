package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth

private val CardWidth = 112.dp
private val CardHeight = 132.dp
private val SelectedBorder = 2.dp

@Composable
internal fun HistoryMonthCard(
    month: HistoryMonth,
    ratio: Float,
    previousRatio: Float?,
    nextRatio: Float?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val total = month.total.formatPrice(LocalCurrency.current)
    val label = month.key.fullLabel()
    val description = stringResource(
        if (selected) R.string.history_month_description_selected else R.string.history_month_description,
        label,
        total
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .width(CardWidth)
            .height(CardHeight)
            .testTag("history_month_${month.key}")
            .semantics {
                this.selected = selected
                contentDescription = description
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface
        ),
        border = if (selected) BorderStroke(SelectedBorder, colors.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp).clearAndSetSemantics { },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = month.key.shortLabel(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) colors.primary else colors.onSurfaceVariant
            )
            HistoryMonthTrend(
                ratio = ratio,
                previousRatio = previousRatio,
                nextRatio = nextRatio,
                color = if (selected) colors.primary else colors.outline
            )
            Text(
                text = total,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
