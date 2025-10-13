package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_LONG
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.constants.SIX_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components.PriceInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizePurchaseScreen(
    cartId: String?,
    navController: NavController,
    viewModel: FinalizePurchaseViewModel = hiltViewModel()
) {
    ModalBottomSheet(
        onDismissRequest = {
            navController.popBackStack()
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        FinalizePurchaseContent(
            onSubmit = { name, price ->
                viewModel.onIntent(
                    intent = FinalizePurchaseUiIntent.FinalizePurchase(
                        cartId = cartId,
                        name = name,
                        price = price
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizePurchaseContent(
    name: String = EMPTY_STRING,
    price: Long = EMPTY_LONG,
    onSubmit: (name: String, price: Long) -> Unit
) {
    var name by remember { mutableStateOf(name) }
    var price by remember { mutableLongStateOf(price) }
    var error by remember { mutableStateOf(false) }

    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.checkout_cart),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = false
            },
            label = { Text(stringResource(R.string.market_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester)
        )

        Spacer(modifier = Modifier.height(8.dp))

        PriceInput(
            price = price,
            onPriceChange = {
                price = it
                error = false
            },
            label = { Text(stringResource(R.string.price_paid)) },
            modifier = Modifier.fillMaxWidth()
        )

        if (error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.message_error_field_required_finaliza_purchase),
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && price > 0L) {
                    onSubmit(name, price)
                } else {
                    error = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.confirm))
        }
    }
}

@Composable
@DevicesPreview
private fun FinalizePurchasePreview() {
    FinalizePurchaseContent(
        onSubmit = { _, _ -> }
    )
}
