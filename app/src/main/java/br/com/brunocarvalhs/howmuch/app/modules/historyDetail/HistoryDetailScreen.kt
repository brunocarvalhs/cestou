package br.com.brunocarvalhs.howmuch.app.modules.historyDetail

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import br.com.brunocarvalhs.howmuch.app.modules.historyDetail.components.HistoryDetailTopBar

@Composable
fun HistoryDetailScreen(
    navController: NavController,
    viewModel: HistoryDetailViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
        }

        if (uiState.isDeleting) {
            navController.popBackStack()
            Toast.makeText(context, "Cart deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }

    HistoryDetailContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun HistoryDetailContent(
    uiState: HistoryDetailUiState,
    onIntent: (HistoryDetailUiIntent) -> Unit = {},
    onBack: () -> Unit = {},
    context: Context = LocalContext.current
) {
    Scaffold(
        topBar = {
            HistoryDetailTopBar(
                title = uiState.cart?.market,
                onBack = onBack,
                onShare = {
                    uiState.cart?.let { cart ->
                        onIntent(HistoryDetailUiIntent.ShareCart(context, cart))
                    }
                },
                onDelete = {
                    uiState.cart?.let { cart ->
                        onIntent(HistoryDetailUiIntent.Delete(cart))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }

                uiState.cart != null -> {
                    val cart = uiState.cart
                    val calculatedTotal = cart.recalculateTotal()
                    val difference = cart.totalPrice - calculatedTotal

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cart.market,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        cart.purchaseDate?.let {
                            Text(
                                text = it,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(cart.products) { product ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = product.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "x${product.quantity}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(40.dp)
                                )
                                Text(
                                    text = product.price?.toCurrencyString().orEmpty(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TOTAL CALCULADO",
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                calculatedTotal.toCurrencyString(),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TOTAL REGISTRADO",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                cart.totalPrice.toCurrencyString(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (difference != 0L) {
                            val color = if (difference > 0) Color.Red else Color(0xFF2E7D32)
                            val label = if (difference > 0) "Diferença (+)" else "Diferença (-)"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    label,
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                                Text(
                                    difference.toCurrencyString(),
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onIntent(HistoryDetailUiIntent.CreateListFromHistory(cart)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.create_list_from_history))
                    }
                }
            }
        }
    }
}
