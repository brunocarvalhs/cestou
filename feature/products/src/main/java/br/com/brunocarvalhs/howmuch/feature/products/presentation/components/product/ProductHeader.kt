package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

/**
 * Just the title + a single nav action now — the option-chip selector was dropped so the layout
 * has one natural flow: [Options.FORM] (manual entry) with a camera shortcut into [Options.PHOTO],
 * instead of a multi-tab picker. [canNavigateBack] swaps the action between "back" (popping to
 * Form from a pushed route like Photo) and "close" (leaving the picker entirely from Form).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductHeader(
    modifier: Modifier = Modifier,
    shoppingTitle: String? = null,
    canNavigateBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .background(
                TopAppBarDefaults.topAppBarColors().containerColor
            )
    ) {
        TopAppBar(
            title = {
                if (!shoppingTitle.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.product_header_add_to, shoppingTitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.content_description_back)
                        )
                    }
                }
            },
            actions = {
                if (!canNavigateBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(CoreR.string.content_description_back)
                        )
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun ProductHeaderPreview() {
    ProductHeader(shoppingTitle = "Compras da semana")
}

@Preview(name = "Rota empilhada (câmera)")
@Composable
private fun ProductHeaderCanNavigateBackPreview() {
    ProductHeader(shoppingTitle = "Compras da semana", canNavigateBack = true)
}
