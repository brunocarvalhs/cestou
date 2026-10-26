package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.spendhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendHistoryPurchase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Date-group header above each day's purchases in the "Compras" list. */
@Composable
internal fun SpendHistoryDateHeader(timestamp: Long, modifier: Modifier = Modifier) {
    Text(
        text = formatDateHeader(timestamp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * One finished purchase row: emoji, list title, establishment (if any), total, and a
 * "compartilhada" indicator for shared lists. Estimated totals (calculated from purchased items
 * rather than a user-confirmed value) are marked with both a "≈" prefix *and* an "estimado"
 * caption — two non-color signals — so a calculated number never reads as confirmed.
 */
@Composable
internal fun SpendHistoryPurchaseListItem(
    purchase: SpendHistoryPurchase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = rememberCurrencyFormatter()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("spend_history_purchase_item_${purchase.id}")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = purchase.emoji ?: Shopping.DEFAULT_EMOJI,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = purchase.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (!purchase.establishment.isNullOrBlank()) {
                Text(
                    text = purchase.establishment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (purchase.isShared) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.spend_history_shared_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            val totalLabel = currencyFormatter.format(purchase.total)
            Text(
                text = if (purchase.isEstimated) {
                    stringResource(R.string.spend_history_estimated_amount_format, totalLabel)
                } else {
                    totalLabel
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (purchase.isEstimated) {
                Text(
                    text = stringResource(R.string.spend_history_estimated_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDateHeader(timestamp: Long): String {
    val formatter = SimpleDateFormat("d MMMM", Locale.getDefault())
    return formatter.format(Date(timestamp)).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

private const val PREVIEW_BASE_TIMESTAMP = 1_750_000_000_000L

@Preview(showBackground = true, name = "Compra confirmada")
@Composable
private fun SpendHistoryPurchaseListItemPreview() {
    CestouTheme {
        SpendHistoryPurchaseListItem(
            purchase = SpendHistoryPurchase(
                id = "1",
                timestamp = PREVIEW_BASE_TIMESTAMP,
                emoji = "🛒",
                title = "Compras do mês",
                establishment = "Supermercado Extra",
                total = 184.70,
                isShared = true
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Compra estimada, sem estabelecimento")
@Composable
private fun SpendHistoryPurchaseListItemEstimatedPreview() {
    CestouTheme {
        SpendHistoryPurchaseListItem(
            purchase = SpendHistoryPurchase(
                id = "2",
                timestamp = PREVIEW_BASE_TIMESTAMP,
                emoji = "🧺",
                title = "Padaria rápida",
                establishment = null,
                total = 15.50,
                isEstimated = true
            ),
            onClick = {}
        )
    }
}
