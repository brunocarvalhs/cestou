package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common.Options
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductHeader(
    modifier: Modifier = Modifier,
    shoppingTitle: String? = null,
    selectedOption: Options = Options.AI,
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
                if (!shoppingTitle.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.product_header_add_to, shoppingTitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
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
                val selected = option == selectedOption
                FilterChip(
                    selected = selected,
                    label = {
                        Text(
                            text = when (option) {
                                Options.SEARCH -> stringResource(R.string.product_option_search)
                                Options.PHOTO -> stringResource(R.string.product_option_photo)
                                Options.SUGGESTIONS -> stringResource(R.string.product_option_suggestions)
                                Options.AI -> stringResource(R.string.product_option_ai)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = option.icon(),
                            contentDescription = null,
                            modifier = Modifier.height(FilterChipDefaults.IconSize)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    onClick = {
                        onOptionSelected(option)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun Options.icon(): ImageVector = when (this) {
    Options.SUGGESTIONS -> Icons.Default.AutoAwesome
    Options.SEARCH -> Icons.Default.Search
    Options.PHOTO -> Icons.Default.CameraAlt
    Options.AI -> Icons.Default.SmartToy
}

@Preview
@Composable
private fun ProductHeaderPreview() {
    ProductHeader(shoppingTitle = "Compras da semana")
}
