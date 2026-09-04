package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.CommonProductIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.QuickAddIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.CommonProductUiState
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.QuickAddUiState
import java.text.NumberFormat

/**
 * [Options.QUICK_ADD]'s content — the new default add surface (spec IAA-03 / `design.md`
 * "Options.QUICK_ADD"). One screen, no further navigation: a persistent total/budget mini header,
 * a text field that saves directly, quick-tap chips for common products (reusing
 * [CommonProductIntent.onAddToShopping] — no new use case), and a shortcut into [Options.PHOTO].
 */
@Composable
internal fun QuickAddForm(
    modifier: Modifier = Modifier,
    uiState: QuickAddUiState,
    intent: QuickAddIntent = QuickAddIntent(),
    commonUiState: CommonProductUiState,
    commonIntent: CommonProductIntent = CommonProductIntent(),
    currencyFormatter: CurrencyFormatter = rememberCurrencyFormatter(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToPhoto: () -> Unit = {}
) {
    LaunchedEffect(commonUiState.message) {
        commonUiState.message?.let {
            snackbarHostState.showSnackbar(it)
            commonIntent.onMessageShown()
        }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbarHostState.showSnackbar(it)
            intent.onSaveErrorShown()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        QuickAddTotalHeader(
            totalAmount = uiState.totalAmount,
            budget = uiState.budget,
            isOverBudget = uiState.isOverBudget,
            currencyFormatter = currencyFormatter
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.newItemName,
                onValueChange = { intent.onNewItemNameChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.quick_add_placeholder)) },
                singleLine = true,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { intent.onSubmit() })
            )
            IconButton(onClick = onNavigateToPhoto, enabled = !uiState.isSaving) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.quick_add_scan_description)
                )
            }
            IconButton(
                onClick = { intent.onSubmit() },
                enabled = uiState.newItemName.isNotBlank() && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.quick_add_add_description),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (uiState.duplicateWarning != null) {
            QuickAddDuplicateWarning(
                message = uiState.duplicateWarning,
                onDismiss = { intent.onDuplicateWarningShown() }
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.quick_add_common_products_title),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        if (commonUiState.isLoading && commonUiState.items.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(start = 20.dp))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = commonUiState.items, key = { it.id }) { item ->
                    AssistChip(
                        onClick = { commonIntent.onAddToShopping(item) },
                        label = { Text(item.name) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ShoppingBasket,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddTotalHeader(
    totalAmount: Double,
    budget: Double?,
    isOverBudget: Boolean,
    currencyFormatter: CurrencyFormatter
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isOverBudget) colorScheme.errorContainer else colorScheme.primaryContainer
    val contentColor = if (isOverBudget) colorScheme.error else colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isOverBudget) {
                    stringResource(R.string.shopping_list_summary_over_budget)
                } else {
                    stringResource(R.string.shopping_list_summary_total, currencyFormatter.format(totalAmount))
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (budget != null) {
                Text(
                    text = "${currencyFormatter.format(totalAmount)} / ${currencyFormatter.format(budget)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun QuickAddDuplicateWarning(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(br.com.brunocarvalhs.howmuch.core.ui.R.string.action_close),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private val previewCommonItems = listOf(
    CommonProduct(id = "1", name = "Arroz", category = "Mercearia", unit = "kg"),
    CommonProduct(id = "2", name = "Feijão", category = "Mercearia", unit = "kg"),
    CommonProduct(id = "3", name = "Leite", category = "Laticínios", unit = "L")
)

@Preview(showBackground = true)
@Composable
private fun QuickAddFormPreview() {
    QuickAddForm(
        uiState = QuickAddUiState(totalAmount = 45.9, budget = 120.0),
        commonUiState = CommonProductUiState(items = previewCommonItems),
        currencyFormatter = CurrencyFormatter(NumberFormat.getCurrencyInstance())
    )
}

@Preview(showBackground = true, name = "Over budget")
@Composable
private fun QuickAddFormOverBudgetPreview() {
    QuickAddForm(
        uiState = QuickAddUiState(totalAmount = 150.0, budget = 120.0),
        commonUiState = CommonProductUiState(items = previewCommonItems),
        currencyFormatter = CurrencyFormatter(NumberFormat.getCurrencyInstance())
    )
}

@Preview(showBackground = true, name = "Duplicate warning")
@Composable
private fun QuickAddFormDuplicateWarningPreview() {
    QuickAddForm(
        uiState = QuickAddUiState(
            newItemName = "",
            totalAmount = 45.9,
            budget = 120.0,
            duplicateWarning = "Leite já está na lista"
        ),
        commonUiState = CommonProductUiState(items = previewCommonItems),
        currencyFormatter = CurrencyFormatter(NumberFormat.getCurrencyInstance())
    )
}
