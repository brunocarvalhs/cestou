package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.cart

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@Composable
internal fun CartAdd(
    onClick: () -> Unit = {}
) {
    FloatingActionButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(CoreR.string.action_add)
        )
    }
}

@Preview
@Composable
private fun CartAddPreview() {
    CartAdd()
}
