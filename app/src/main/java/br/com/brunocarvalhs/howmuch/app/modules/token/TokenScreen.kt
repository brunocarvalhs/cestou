package br.com.brunocarvalhs.howmuch.app.modules.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.constants.FIVE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.SIX_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartUiIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenScreen(
    token: String? = null,
    navController: NavController,
    viewModel: TokenViewModel = hiltViewModel()
) {
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = { navController.popBackStack() },
    ) {
        TokenContent(
            onDismiss = {
                navController.popBackStack()
                trackClick(
                    viewId = "token_sheet_dismiss",
                    viewName = "Dismiss Token Sheet",
                    screenName = "ShoppingCartScreen"
                )
            },
            onSubmit = { code ->
                viewModel.onIntent(TokenUiIntent.SearchByToken(token = code))
                trackClick(
                    viewId = "token_sheet_submit",
                    viewName = "Submit Token",
                    screenName = "ShoppingCartScreen"
                )
            }
        )
    }
}

@Composable
fun TokenContent(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var digits by remember { mutableStateOf(List(SIX_INT) { EMPTY_STRING }) }

    val focusRequesters = List(SIX_INT) { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesters[ZERO_INT].requestFocus()
    }


    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.enter_the_digit_code, SIX_INT))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            digits.forEachIndexed { index, value ->
                TextField(
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.length <= ONE_INT && newValue.all { it.isDigit() }) {
                            digits = digits.toMutableList().also { it[index] = newValue }
                            if (newValue.isNotEmpty() && index < FIVE_INT) {
                                focusRequesters[index + ONE_INT].requestFocus()
                            }
                        } else if (newValue.isEmpty() && index > ZERO_INT) {
                            digits = digits.toMutableList().also { it[index] = EMPTY_STRING }
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

        Button(
            onClick = {
                val code = digits.joinToString(EMPTY_STRING)
                if (code.length == SIX_INT) {
                    onSubmit(code)
                    onDismiss()
                    keyboardController?.hide()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.access))
        }
    }
}

@Composable
@Preview
fun TokenBottomSheetPreview() {
    TokenContent(
        onDismiss = {},
        onSubmit = {}
    )
}