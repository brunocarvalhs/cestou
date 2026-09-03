package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatQuantity
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.feature.products.R

private const val CURRENCY_DIVISOR = 100.0
private const val MAX_PRICE_LENGTH = 12
private const val QUANTITY_FIELD_WEIGHT = 0.6f

@Composable
internal fun ConfirmItemContent(
    product: Product,
    onConfirm: (Double?, Double) -> Unit
) {
    val currencyFormatter = rememberCurrencyFormatter()
    var price by remember {
        mutableStateOf(
            value = ((product.price ?: 0.0) * CURRENCY_DIVISOR).toLong().toString()
        )
    }
    var quantity by remember { mutableStateOf(product.quantity.formatQuantity()) }
    val visualTransformation = rememberCurrencyVisualTransformation()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.shopping_list_confirm_item_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = price,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        price = it.take(MAX_PRICE_LENGTH)
                    }
                },
                label = { Text(stringResource(R.string.shopping_list_label_unit_price)) },
                prefix = { Text(currencyFormatter.currency?.symbol ?: "R$") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = visualTransformation,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text(stringResource(R.string.shopping_list_label_quantity)) },
                modifier = Modifier.weight(QUANTITY_FIELD_WEIGHT),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val finalPrice = price.toDoubleOrNull()?.div(CURRENCY_DIVISOR) ?: product.price
                val finalQuantity = quantity.toDoubleOrNull() ?: product.quantity
                onConfirm(finalPrice, finalQuantity)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.action_confirm))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmItemContentPreview() {
    MaterialTheme {
        ConfirmItemContent(
            product = Product(
                id = "1",
                name = "Produto de Exemplo",
                quantity = 2.0,
                price = 10.0
            ),
            onConfirm = { _, _ -> }
        )
    }
}
