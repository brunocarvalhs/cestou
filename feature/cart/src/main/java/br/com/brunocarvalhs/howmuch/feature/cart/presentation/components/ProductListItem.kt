package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextPrimary
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextSecondary

@Composable
internal fun ProductListItem(
    product: Product,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onQuantityChange: (Double) -> Unit,
    onTogglePurchased: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = true
) {
    val currencyFormatter = rememberCurrencyFormatter()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onTogglePurchased(!product.isPurchased) },
                enabled = enabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (product.isPurchased) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (product.isPurchased) CestouBrightGreen else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (product.isPurchased) Color.LightGray else CestouTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (product.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${product.quantity} ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            Text(
                text = currencyFormatter.format(product.total),
                style = MaterialTheme.typography.bodyLarge,
                color = CestouTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}
