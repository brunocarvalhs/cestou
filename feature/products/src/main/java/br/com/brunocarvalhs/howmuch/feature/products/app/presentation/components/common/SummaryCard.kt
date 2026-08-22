package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.products.R
import java.text.NumberFormat

private const val OVER_BUDGET_ALPHA = 0.6f
private const val PRIMARY_CONTAINER_ALPHA = 0.4f
private const val ON_SURFACE_VARIANT_ALPHA = 0.7f
private const val BUDGET_ALPHA = 0.8f
private const val TRACK_COLOR_ALPHA = 0.2f

@Composable
internal fun SummaryCard(
    totalAmount: Double,
    cartAmount: Double,
    itemCount: Int,
    purchasedCount: Int,
    currencyFormatter: CurrencyFormatter,
    budget: Double? = null
) {
    val isOverBudget = budget != null && totalAmount > budget
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = remember(isOverBudget, colorScheme) {
        if (isOverBudget) {
            colorScheme.errorContainer.copy(alpha = OVER_BUDGET_ALPHA)
        } else {
            colorScheme.primaryContainer.copy(alpha = PRIMARY_CONTAINER_ALPHA)
        }
    }
    val contentColor = remember(isOverBudget, colorScheme) {
        if (isOverBudget) {
            colorScheme.error
        } else {
            colorScheme.primary
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOverBudget) {
                        stringResource(R.string.shopping_list_summary_over_budget)
                    } else {
                        stringResource(R.string.shopping_list_summary_in_cart)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currencyFormatter.format(cartAmount),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.shopping_list_summary_total,
                            currencyFormatter.format(totalAmount)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ON_SURFACE_VARIANT_ALPHA)
                    )
                    if (budget != null) {
                        Text(
                            text = " / ${currencyFormatter.format(budget)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = BUDGET_ALPHA),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.shopping_list_summary_items_count, purchasedCount, itemCount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Text(
                    text = stringResource(R.string.shopping_list_summary_items_in_cart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ON_SURFACE_VARIANT_ALPHA)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (itemCount > 0) purchasedCount.toFloat() / itemCount else 0f },
                    modifier = Modifier
                        .width(80.dp)
                        .height(6.dp),
                    strokeCap = StrokeCap.Round,
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = TRACK_COLOR_ALPHA)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardPreview() {
    MaterialTheme {
        SummaryCard(
            totalAmount = 500.0,
            cartAmount = 120.50,
            itemCount = 20,
            purchasedCount = 8,
            currencyFormatter = CurrencyFormatter(NumberFormat.getCurrencyInstance()),
            budget = 600.0
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardOverBudgetPreview() {
    MaterialTheme {
        SummaryCard(
            totalAmount = 750.0,
            cartAmount = 300.0,
            itemCount = 15,
            purchasedCount = 5,
            currencyFormatter = CurrencyFormatter(NumberFormat.getCurrencyInstance()),
            budget = 500.0
        )
    }
}
