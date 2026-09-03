package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.feature.products.R

private const val CURRENCY_DIVISOR = 100.0
private const val MAX_PRICE_LENGTH = 12

@Composable
internal fun FinishPurchaseContent(
    totalEstimate: Double,
    onFinish: (Double, String) -> Unit
) {
    val currencyFormatter = rememberCurrencyFormatter()
    var price by remember { mutableStateOf((totalEstimate * CURRENCY_DIVISOR).toLong().toString()) }
    var establishment by remember { mutableStateOf("") }
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
            text = stringResource(R.string.shopping_list_finish_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.shopping_list_finish_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = price,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    price = it.take(MAX_PRICE_LENGTH)
                }
            },
            label = { Text(stringResource(R.string.shopping_list_label_total_value)) },
            prefix = { Text(currencyFormatter.currency?.symbol ?: "R$") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = establishment,
            onValueChange = { establishment = it },
            label = { Text(stringResource(R.string.shopping_list_label_establishment)) },
            placeholder = { Text(stringResource(R.string.shopping_list_placeholder_establishment)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val finalPrice = price.toDoubleOrNull()?.div(CURRENCY_DIVISOR) ?: totalEstimate
                onFinish(finalPrice, establishment)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.shopping_list_confirm_save))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun FinishPurchaseContentPreview() {
    MaterialTheme {
        FinishPurchaseContent(
            totalEstimate = 150.50,
            onFinish = { _, _ -> }
        )
    }
}
