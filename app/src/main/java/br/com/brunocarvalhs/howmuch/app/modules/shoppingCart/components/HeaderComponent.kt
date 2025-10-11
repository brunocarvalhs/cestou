package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderComponent(
    title: String? = null,
    onShared: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            title?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            onShared?.let {
                IconButton(onClick = it) {
                    Icon(
                        painterResource(R.drawable.ic_add_shopping_cart),
                        contentDescription = "Share"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
@Preview
fun HeaderComponentPreview() {
    HeaderComponent(
        title = "Shopping Cart",
        onShared = {}
    )
}
