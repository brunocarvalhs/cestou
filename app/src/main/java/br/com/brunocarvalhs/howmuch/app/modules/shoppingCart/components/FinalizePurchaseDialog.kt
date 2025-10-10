package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING

@Composable
fun FinalizePurchaseDialog(
    totalPrice: Long,
    onDismiss: () -> Unit,
    onSubmit: (name: String, price: Long) -> Unit
) {
    var name by remember { mutableStateOf(EMPTY_STRING) }
    var price by remember { mutableLongStateOf(totalPrice) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checkout_cart)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = false
                    },
                    label = { Text(stringResource(R.string.market_name)) },
                )
                PriceInput(
                    price = price,
                    onPriceChange = {
                        price = it
                        error = false
                    },
                    label = { Text(stringResource(R.string.price_paid)) },
                )
                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.message_error_field_required_finaliza_purchase),
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && price > 0L) {
                    onSubmit(name, price)
                    onDismiss()
                } else {
                    error = true
                }
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}