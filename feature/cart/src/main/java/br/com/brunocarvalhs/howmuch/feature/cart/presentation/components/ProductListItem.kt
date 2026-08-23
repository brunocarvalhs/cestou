package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter

@Composable
internal fun ProductListItem(
    modifier: Modifier = Modifier,
    product: Product,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onQuantityChange: (Double) -> Unit = { _ -> },
    onTogglePurchased: (Boolean) -> Unit = { },
    enabled: Boolean = true,
    showDivider: Boolean = true
) {
    val currencyFormatter = rememberCurrencyFormatter()

    Column(modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            enabled = enabled,
            onClick = { onTogglePurchased(!product.isPurchased) },
            onLongClick = { onEdit.invoke() }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = product.isPurchased,
                onCheckedChange = {
                    onTogglePurchased(!product.isPurchased)
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (product.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${product.quantity} ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = currencyFormatter.format(product.total),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Preview
@Composable
private fun ProductListItemPreview() {
    ProductListItem(
        product = Product(
            id = "",
            name = "Arroz",
            quantity = 2.0,
            price = 2.50
        ),
    )
}
