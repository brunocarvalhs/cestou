package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouSoftGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextPrimary
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextSecondary
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter

@Composable
internal fun CartBottomBar(
    modifier: Modifier = Modifier,
    currencyFormatter: CurrencyFormatter,
    totalAmount: Double,
    onFinished: () -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = modifier
            .background(TopAppBarDefaults.topAppBarColors().containerColor)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalButton(
                onClick = onFinished,
                modifier = Modifier.height(56.dp).weight(3f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Checkout", fontWeight = FontWeight.Bold)
                    Text(
                        text = currencyFormatter.format(totalAmount),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}

@Preview
@Composable
private fun CartBottomBarPreview() {
    CartBottomBar(
        currencyFormatter = rememberCurrencyFormatter(),
        totalAmount = 0.0,
        onFinished = {},
        onAdd = {}
    )
}