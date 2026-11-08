package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.cart.R

@Composable
internal fun CartBottomBar(
    modifier: Modifier = Modifier,
    currencyFormatter: CurrencyFormatter,
    totalAmount: Double,
    cartAmount: Double? = null,
    hasUnpricedItems: Boolean = false,
    onFinished: () -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = modifier
            .background(TopAppBarDefaults.topAppBarColors().containerColor)
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        if (cartAmount != null) {
            CartTotalsRow(
                currencyFormatter = currencyFormatter,
                plannedAmount = totalAmount,
                cartAmount = cartAmount,
                hasUnpricedItems = hasUnpricedItems
            )
            Spacer(Modifier.height(12.dp))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalButton(
                onClick = onFinished,
                modifier = Modifier.height(56.dp).weight(PRIMARY_BUTTON_WEIGHT).testTag("cart_finish_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.cart_checkout), fontWeight = FontWeight.Bold)
                    Text(
                        text = currencyFormatter.format(totalAmount),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier.height(56.dp).weight(1f).testTag("cart_add_product_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}

@Composable
private fun CartTotalsRow(
    currencyFormatter: CurrencyFormatter,
    plannedAmount: Double,
    cartAmount: Double,
    hasUnpricedItems: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TotalColumn(
                label = stringResource(R.string.cart_total_planned),
                value = currencyFormatter.format(plannedAmount),
                tag = "cart_total_planned"
            )
            TotalColumn(
                label = stringResource(R.string.cart_total_in_cart),
                value = currencyFormatter.format(cartAmount),
                tag = "cart_total_in_cart",
                horizontalAlignment = Alignment.End
            )
        }
        if (hasUnpricedItems) {
            Text(
                text = stringResource(R.string.cart_total_unpriced_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).testTag("cart_total_unpriced_hint")
            )
        }
    }
}

@Composable
private fun TotalColumn(
    label: String,
    value: String,
    tag: String,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Preview
@Composable
private fun CartBottomBarPreview() {
    CartBottomBar(
        currencyFormatter = rememberCurrencyFormatter(),
        totalAmount = 0.0,
        cartAmount = 0.0,
        onFinished = {},
        onAdd = {}
    )
}

private const val PRIMARY_BUTTON_WEIGHT = 3f
