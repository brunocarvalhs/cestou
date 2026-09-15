package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductFormIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductFormUiState

private const val QUANTITY_FIELD_WEIGHT = 0.6f

/**
 * Backs [Options.FORM], the layout's single natural entry flow: fill in name/quantity/price
 * manually and save, or drop straight into [Options.PHOTO] via the camera shortcut below the
 * form — no tab picker in between. Driven by [ProductFormViewModel] / [ProductFormUiState] /
 * [ProductFormIntent].
 */
@Composable
internal fun FormProduct(
    modifier: Modifier = Modifier,
    uiState: ProductFormUiState,
    intent: ProductFormIntent = ProductFormIntent(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    shoppingTitle: String? = null,
    onNavigateToPhoto: () -> Unit = {},
    onOpenCategoryPicker: () -> Unit = {}
) {
    val currencyFormatter = rememberCurrencyFormatter()
    val visualTransformation = rememberCurrencyVisualTransformation()

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbarHostState.showSnackbar(it)
            intent.onSaveErrorShown()
        }
    }

    LaunchedEffect(uiState.duplicateWarning) {
        uiState.duplicateWarning?.let {
            snackbarHostState.showSnackbar(it)
            intent.onDuplicateWarningShown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = shoppingTitle
                ?.takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.product_header_add_to, it) }
                ?: stringResource(R.string.product_form_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = intent.onNameChange,
            label = { Text(stringResource(R.string.shopping_list_label_product_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isSaving,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val suggestedCategory = remember(uiState.category) { ProductCategory.fromString(uiState.category) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.category,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.shopping_list_label_category)) },
                placeholder = {
                    Text(stringResource(br.com.brunocarvalhs.howmuch.core.ui.R.string.category_picker_title))
                },
                leadingIcon = {
                    Icon(
                        imageVector = suggestedCategory.icon,
                        contentDescription = null,
                        tint = if (uiState.category.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            suggestedCategory.color
                        }
                    )
                },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(12.dp)
            )
            // OutlinedTextField has no onClick of its own; a transparent overlay is the
            // standard way to make a read-only field open a picker instead of the keyboard.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !uiState.isSaving,
                        onClick = onOpenCategoryPicker
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.quantity,
                onValueChange = intent.onQuantityChange,
                label = { Text(stringResource(R.string.shopping_list_label_quantity)) },
                modifier = Modifier.weight(QUANTITY_FIELD_WEIGHT),
                singleLine = true,
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = uiState.price,
                onValueChange = intent.onPriceChange,
                label = { Text(stringResource(R.string.shopping_list_label_unit_price)) },
                prefix = { Text(currencyFormatter.currency?.symbol ?: "R$") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = visualTransformation,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        CestouButton(
            text = stringResource(R.string.product_form_save),
            onClick = intent.onSave,
            enabled = uiState.isValid && !uiState.isSaving
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.product_form_divider_or),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        CestouButton(
            text = stringResource(R.string.product_form_camera_button),
            onClick = onNavigateToPhoto,
            enabled = !uiState.isSaving,
            trailingIcon = Icons.Default.CameraAlt,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun FormProductPreview() {
    FormProduct(uiState = ProductFormUiState())
}

@Preview(showBackground = true, name = "Preenchido")
@Composable
private fun FormProductFilledPreview() {
    FormProduct(
        uiState = ProductFormUiState(
            name = "Arroz Branco 5kg",
            quantity = "2",
            price = "2590",
            category = "Mercearia"
        )
    )
}

@Preview(showBackground = true, name = "Salvando")
@Composable
private fun FormProductSavingPreview() {
    FormProduct(
        uiState = ProductFormUiState(
            name = "Arroz Branco 5kg",
            quantity = "2",
            price = "2590",
            isSaving = true
        )
    )
}
