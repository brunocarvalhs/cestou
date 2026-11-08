package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.ChainQuote
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketPriceComparison
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.PriceComparatorIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.PriceComparatorUiState

/** Stateless: the caller owns the ViewModel, so this is what tests and previews render. */
@Composable
internal fun PriceComparatorScreen(
    state: PriceComparatorUiState,
    intent: PriceComparatorIntent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("price_comparator_screen")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.price_comparator_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.price_comparator_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("price_comparator_disclaimer")
        )
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("price_comparator_loading")
            )
            state.error != null -> Text(
                text = state.error.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("price_comparator_error")
            )
            state.showLocationForm -> LocationForm(state, intent)
            state.comparison != null -> ComparisonResult(state.comparison, intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationForm(state: PriceComparatorUiState, intent: PriceComparatorIntent) {
    var uf by rememberSaveable { mutableStateOf(state.location?.state.orEmpty()) }
    var city by rememberSaveable { mutableStateOf(state.location?.city.orEmpty()) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.testTag("price_comparator_location_form"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.price_comparator_location_title),
            style = MaterialTheme.typography.titleMedium
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = uf,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.price_comparator_state_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag("price_comparator_state_field")
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                UserLocation.BRAZILIAN_STATES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            uf = option
                            expanded = false
                        },
                        modifier = Modifier.testTag("price_comparator_state_$option")
                    )
                }
            }
        }
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            singleLine = true,
            label = { Text(stringResource(R.string.price_comparator_city_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("price_comparator_city_field")
        )
        CestouButton(
            text = stringResource(R.string.price_comparator_location_save),
            onClick = { intent.onSaveLocation(uf, city) },
            enabled = uf.isNotBlank() && city.isNotBlank(),
            modifier = Modifier.testTag("price_comparator_save_location")
        )
    }
}

@Composable
private fun ComparisonResult(comparison: SupermarketPriceComparison, intent: PriceComparatorIntent) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ComparisonContent(comparison, intent)
    }
}

@Composable
private fun ColumnScope.ComparisonContent(comparison: SupermarketPriceComparison, intent: PriceComparatorIntent) {
    val currency = LocalCurrency.current
    TextButton(
        onClick = intent.onChangeLocation,
        modifier = Modifier.testTag("price_comparator_change_location")
    ) {
        Text(
            stringResource(
                R.string.price_comparator_location_change,
                comparison.location.city,
                comparison.location.state
            )
        )
    }
    when {
        comparison.baseTotal <= 0.0 -> Text(
            text = stringResource(R.string.price_comparator_empty_prices),
            modifier = Modifier.testTag("price_comparator_no_prices")
        )
        comparison.quotes.isEmpty() -> Text(
            text = stringResource(R.string.price_comparator_no_chains),
            modifier = Modifier.testTag("price_comparator_no_chains")
        )
        else -> {
            Text(
                text = stringResource(
                    R.string.price_comparator_savings_headline,
                    comparison.maxSavings.formatPrice(currency)
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("price_comparator_headline")
            )
            comparison.quotes.forEachIndexed { index, quote ->
                QuoteRow(quote = quote, isCheapest = index == 0)
            }
        }
    }
    if (comparison.unpricedCount > 0) {
        Text(
            text = stringResource(R.string.price_comparator_unpriced, comparison.unpricedCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("price_comparator_unpriced")
        )
    }
}

@Composable
private fun QuoteRow(quote: ChainQuote, isCheapest: Boolean) {
    val currency = LocalCurrency.current
    CestouCard(modifier = Modifier.fillMaxWidth().testTag("price_comparator_quote_${quote.chain.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quote.chain.name, style = MaterialTheme.typography.titleMedium)
                if (isCheapest) {
                    Text(
                        text = stringResource(R.string.price_comparator_cheapest),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("price_comparator_cheapest_badge")
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    quote.estimatedTotal.formatPrice(currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (quote.savings > 0.0) {
                    Text(
                        text = stringResource(
                            R.string.price_comparator_savings_chain,
                            quote.savings.formatPrice(currency)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
