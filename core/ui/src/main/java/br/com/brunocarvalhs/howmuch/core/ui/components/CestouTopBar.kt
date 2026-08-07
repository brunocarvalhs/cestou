package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CestouTopBar(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit),
    actions: @Composable (RowScope.() -> Unit) = {},
    navigationIcon: @Composable (() -> Unit) = {}
) {
    TopAppBar(
        title = title,
        actions = actions,
        navigationIcon = navigationIcon
    )
}

@Preview
@Composable
private fun TopBarPreview() {

}
