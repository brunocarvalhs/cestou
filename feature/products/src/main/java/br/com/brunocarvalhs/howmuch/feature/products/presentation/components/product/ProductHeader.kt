package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.Options
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductHeader(
    modifier: Modifier = Modifier,
    selectedOption: Options = Options.BARCODE,
    onOptionSelected: (Options) -> Unit = {},
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

            },
            actions = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(CoreR.string.content_description_back)
                    )
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Options.entries) { option ->
                FilterChip(
                    selected = option == selectedOption,
                    label = {
                        Text(
                            text = when (option) {
                                Options.BARCODE -> stringResource(R.string.product_option_barcode)
                                Options.SEARCH -> stringResource(R.string.product_option_search)
                                Options.PHOTO -> stringResource(R.string.product_option_photo)
                                Options.SUGGESTIONS -> stringResource(R.string.product_option_suggestions)
                            }
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview
@Composable
private fun ProductHeaderPreview() {
    ProductHeader()
}
