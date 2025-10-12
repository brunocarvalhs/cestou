package br.com.brunocarvalhs.howmuch.app.modules.token.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.constants.FIVE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT

@Composable
fun InputCode(
    digits: List<String>,
    onDigitsChange: (List<String>) -> Unit,
    focusRequesters: List<FocusRequester>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        digits.forEachIndexed { index, value ->
            TextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.length <= ONE_INT && newValue.all { it.isDigit() }) {
                        onDigitsChange(digits.toMutableList().also { it[index] = newValue })
                        if (newValue.isNotEmpty() && index < FIVE_INT) {
                            focusRequesters[index + ONE_INT].requestFocus()
                        }
                    } else if (newValue.isEmpty() && index > ZERO_INT) {
                        onDigitsChange(digits.toMutableList().also { it[index] = EMPTY_STRING })
                        focusRequesters[index - ONE_INT].requestFocus()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(focusRequesters[index]),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (index == FIVE_INT) ImeAction.Done else ImeAction.Next
                )
            )
        }
    }
}

@Composable
@DevicesPreview
fun InputCodePreview() {
    InputCode(
        digits = List(FIVE_INT) { EMPTY_STRING },
        onDigitsChange = {},
        focusRequesters = List(FIVE_INT) { FocusRequester() }
    )
}