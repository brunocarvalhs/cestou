package br.com.brunocarvalhs.howmuch.app.modules.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.constants.FIVE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.SIX_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartUiIntent
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartUiState
import br.com.brunocarvalhs.howmuch.app.modules.token.components.InputCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenScreen(
    navController: NavController,
    viewModel: TokenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = { navController.popBackStack() },
    ) {
        TokenContent(
            uiState = uiState,
            onDismiss = {
                navController.popBackStack()
                trackClick(
                    viewId = "token_sheet_dismiss",
                    viewName = "Dismiss Token Sheet",
                    screenName = "ShoppingCartScreen"
                )
            },
            onSubmit = { code ->
                viewModel.onIntent(TokenUiIntent.SearchByToken)
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
    uiState: TokenUiState,
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
            InputCode(
                digits = digits,
                onDigitsChange = { newDigits -> digits = newDigits },
                focusRequesters = focusRequesters
            )
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
            modifier = Modifier.fillMaxWidth(),
            enabled = digits.all { it.isNotEmpty() } && uiState !is TokenUiState.Loading
        ) {
            if (uiState is TokenUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.access))
            }
        }
    }
}

private class TokenStateProvider : PreviewParameterProvider<TokenUiState> {
    override val values: Sequence<TokenUiState>
        get() = sequenceOf(
            TokenUiState.Idle,
            TokenUiState.Loading,
            TokenUiState.Success(cartId = "123456"),
            TokenUiState.Error(message = "Error"),
        )
}

@Composable
@DevicesPreview
fun TokenBottomSheetPreview(
    @PreviewParameter(TokenStateProvider::class) uiState: TokenUiState
) {
    TokenContent(
        uiState = uiState,
        onDismiss = {},
        onSubmit = {},
    )
}