package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_LONG
import br.com.brunocarvalhs.howmuch.app.modules.products.components.PriceInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceFieldBottomSheet(
    onDismissRequest: () -> Unit = {},
    onConfirmation: (Long) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        PriceFieldContent(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation
        )
    }
}

@Composable
private fun PriceFieldContent(
    onDismissRequest: () -> Unit,
    onConfirmation: (Long) -> Unit
) {
    var price by remember { mutableLongStateOf(EMPTY_LONG) }

    Column {
        PriceInput(
            price = price,
            onPriceChange = { price = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.action_cancel))
            }

            Button(onClick = { onConfirmation.invoke(price) }) {
                Text(text = stringResource(id = R.string.action_confirm))
            }
        }
    }
}

@Composable
@Preview
private fun PriceFieldBottomSheetPreview() {
    PriceFieldContent(
        onDismissRequest = {},
        onConfirmation = {}
    )
}
