package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalNight
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.cart.R

/**
 * Barra inferior da lista aberta (nova identidade): um único botão de largura total com o
 * total à direita, no padrão do canvas. Adicionar itens fica no campo no topo da lista.
 */
@Composable
internal fun CartBottomBar(
    currencyFormatter: CurrencyFormatter,
    totalAmount: Double,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Button(
            onClick = onFinished,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(52.dp)
                .testTag("cart_finish_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SubtotalNight,
                contentColor = Color.White
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.cart_checkout), fontWeight = FontWeight.Bold)
                Text(
                    text = currencyFormatter.format(totalAmount),
                    fontWeight = FontWeight.Black
                )
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
        onFinished = {}
    )
}
