package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.TWO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.theme.HowMuchTheme
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartUiState

@Composable
fun ShoppingCartCardsPager(
    uiState: ShoppingCartUiState,
    onCheckout: () -> Unit = {},
    onShared: () -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = ZERO_INT,
        pageCount = { TWO_INT }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) { page ->
            when (page) {
                ZERO_INT -> ShoppingCartInfo(
                    totalPrice = uiState.totalPrice,
                    productsCount = uiState.products.size,
                    isLoading = uiState.isLoading,
                    onCheckout = onCheckout,
                    onShared = onShared,
                    modifier = Modifier.fillMaxWidth()
                )

                ONE_INT -> ShoppingCartLimitCard(
                    totalSpent = uiState.totalPrice,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(pagerState.pageCount) { index ->
                val color =
                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(color = color, shape = CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
@Preview
private fun ShoppingCartCardsPagerPreview() {
    HowMuchTheme {
        ShoppingCartCardsPager(
            uiState = ShoppingCartUiState(),
            onCheckout = {},
            onShared = {}
        )
    }
}
