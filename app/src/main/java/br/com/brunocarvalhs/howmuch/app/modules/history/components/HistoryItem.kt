package br.com.brunocarvalhs.howmuch.app.modules.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.data.model.ShoppingCartModel
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.DateFormat
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toFormatDate
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toLocalDate
import java.time.LocalDate

@Composable
fun HistoryItem(
    item: ShoppingCart,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val isCurrentMonth = remember(item.purchaseDate) {
        val purchaseDate = item.purchaseDate.toLocalDate()
        purchaseDate?.let {
            it.monthValue == LocalDate.now().monthValue && it.year == LocalDate.now().year
        } ?: false
    }
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        onClick = {
            onClick?.invoke()
            trackClick(
                viewId = "history_item",
                viewName = "History Item",
                screenName = "HistoryScreen"
            )
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        onCheckedChange?.invoke(checked)
                        trackClick(
                            viewId = "history_item_checkbox",
                            viewName = "Checkbox",
                            screenName = "HistoryScreen"
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Column {
                Text(
                    text = item.market,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.currency, item.totalPrice.toCurrencyString()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        trackClick(
                            viewId = "history_item_date",
                            viewName = "AssistChip Date",
                            screenName = "HistoryScreen"
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        labelColor = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    label = {
                        Text(
                            text = item.purchaseDate.toFormatDate(
                                outputFormat = DateFormat.MONTH_NAME_YEAR
                            )
                        )
                    }
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
@Preview
fun HistoryItemPreview() {
    HistoryItem(
        item = ShoppingCartModel(
            id = "1",
            market = "Supermarket",
            totalPrice = 15000L,
            purchaseDate = "2023-10-10",
        ),
    )
}
