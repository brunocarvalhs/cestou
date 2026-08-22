package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency

@Composable
internal fun CartProductItem(
    modifier: Modifier = Modifier,
    name: String = "Arroz",
    quantity: Int = 1,
    price: Double = 10.0,
    total: Double = 10.0,
    isPurchased: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = isPurchased,
                onCheckedChange = { onCheckedChange.invoke(it) }
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val currency = LocalCurrency.current
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "$quantity un • ${price.formatPrice(currency)} cada",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = total.formatPrice(LocalCurrency.current),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview
@Composable
private fun CartProductItemPreview() {
    CartProductItem()
}
