package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.feature.products.R

private const val CURRENCY_DIVISOR = 100.0
private const val MAX_PRICE_LENGTH = 12
private const val QUANTITY_FIELD_WEIGHT = 0.6f
private const val UNIT_FIELD_WEIGHT = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditItemContent(
    product: Product,
    onSave: (String, String, Double, Double, String) -> Unit
) {
    val currencyFormatter = rememberCurrencyFormatter()
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var price by remember { mutableStateOf((product.price * CURRENCY_DIVISOR).toLong().toString()) }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }
    var unit by remember { mutableStateOf(product.unit) }
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
            text = stringResource(R.string.shopping_list_edit_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.shopping_list_label_product_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text(stringResource(R.string.shopping_list_label_category)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(Icons.Default.Category, null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            var expanded by remember { mutableStateOf(false) }
            val units = listOf("un", "kg", "g", "L", "ml", "cx", "pct")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(UNIT_FIELD_WEIGHT)
            ) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.shopping_list_label_unit)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    units.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                unit = selectionOption
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val finalPrice = price.toDoubleOrNull()?.div(CURRENCY_DIVISOR) ?: product.price
                val finalQuantity = quantity.toDoubleOrNull() ?: product.quantity
                onSave(name, category, finalPrice, finalQuantity, unit)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.shopping_list_save_changes))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun EditItemContentPreview() {
    MaterialTheme {
        EditItemContent(
            product = Product(
                id = "1",
                name = "Produto de Exemplo",
                quantity = 2.0,
                price = 10.0,
                category = "Mercearia",
                unit = "kg"
            ),
            onSave = { _, _, _, _, _ -> }
        )
    }
}
