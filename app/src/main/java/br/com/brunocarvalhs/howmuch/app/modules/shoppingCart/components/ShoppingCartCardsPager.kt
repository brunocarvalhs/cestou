package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.TWO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.theme.HowMuchTheme
import br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.ShoppingCartUiState

private val TABLET_BREAKPOINT = 600.dp

@Composable
fun ShoppingCartCardsPager(
    uiState: ShoppingCartUiState,
    type: Int = ZERO_INT,
    enabledCheckout: Boolean = true,
    enabledShared: Boolean = true,
    onCheckout: () -> Unit = {},
    onShared: (() -> Unit)? = null,
    onLimit: (Long) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        if (maxWidth > TABLET_BREAKPOINT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShoppingCartInfo(
                    totalPrice = uiState.totalPrice,
                    productsCount = uiState.products.size,
                    isLoading = uiState.isLoading,
                    enabledCheckout = enabledCheckout,
                    enabledShared = enabledShared,
                    onCheckout = onCheckout,
                    onShared = onShared,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                ShoppingCartLimitCard(
                    totalSpent = uiState.totalPrice,
                    limit = uiState.limitPrice,
                    onLimit = onLimit,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = type,
                pageCount = { TWO_INT }
            )

            Column(
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
                            enabledCheckout = enabledCheckout,
                            enabledShared = enabledShared,
                            onCheckout = onCheckout,
                            onShared = onShared,
                            modifier = Modifier.fillMaxSize()
                        )

                        ONE_INT -> ShoppingCartLimitCard(
                            totalSpent = uiState.totalPrice,
                            limit = uiState.limitPrice,
                            onLimit = onLimit,
                            modifier = Modifier.fillMaxSize()
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
                            if (pagerState.currentPage == index) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                            }

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
    }
}

@Composable
@DevicesPreview
private fun ShoppingCartCardsPagerPreview() {
    HowMuchTheme {
        ShoppingCartCardsPager(
            uiState = ShoppingCartUiState(),
            onCheckout = {},
            onShared = {}
        )
    }
}

@Composable
@DevicesPreview
private fun ShoppingCartCardsPagerNotSharedPreview() {
    HowMuchTheme {
        ShoppingCartCardsPager(
            uiState = ShoppingCartUiState(),
            onCheckout = {},
        )
    }
}

@Composable
@DevicesPreview
private fun ShoppingCartCardsPagerCardPreview() {
    HowMuchTheme {
        ShoppingCartCardsPager(
            uiState = ShoppingCartUiState(),
            type = ONE_INT,
            onCheckout = {},
            onShared = {}
        )
    }
}
