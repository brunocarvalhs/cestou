package br.com.brunocarvalhs.howmuch.app.modules.products.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_LONG
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.formatted.CurrencyVisualTransformation

@Composable
fun PriceInput(
    price: Long, // preço em centavos
    onPriceChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onNext: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var textValue by remember { mutableStateOf(price.toString()) }

    LaunchedEffect(price) {
        textValue = if (price > EMPTY_LONG) price.toString() else EMPTY_STRING
    }

    OutlinedTextField(
        enabled = enabled,
        value = textValue,
        onValueChange = { newValue ->
            val clean = newValue.replace("[^0-9]".toRegex(), EMPTY_STRING)
            val parsed = clean.toLongOrNull() ?: EMPTY_LONG

            textValue = newValue
            onPriceChange(parsed)
        },
        label = { Text(stringResource(R.string.price)) },
        singleLine = true,
        modifier = modifier.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        visualTransformation = CurrencyVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() }
        ),
        trailingIcon = {
            if (textValue.isNotEmpty()) {
                IconButton(onClick = {
                    textValue = EMPTY_STRING
                    onPriceChange(EMPTY_LONG)
                }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clean_price))
                }
            }
        }
    )
}
