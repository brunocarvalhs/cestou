package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatQuantity
import br.com.brunocarvalhs.howmuch.feature.products.R

private const val QUANTITY_STEP = 1.0
private const val MIN_QUANTITY = 1.0

@Composable
internal fun ProductAnalysisConfirmation(
    modifier: Modifier = Modifier,
    products: List<Product>,
    onUpdateItem: (Product) -> Unit,
    onRemoveItem: (String) -> Unit,
    onConfirmAll: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            Text(
                text = stringResource(R.string.product_photo_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.product_photo_confirm_subtitle,
                    products.size,
                    products.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = products, key = { it.id }) { product ->
                AnalysisResultItem(
                    product = product,
                    onUpdate = onUpdateItem,
                    onRemove = { onRemoveItem(product.id) }
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            CestouButton(
                text = stringResource(R.string.product_photo_confirm_add_all, products.size),
                onClick = onConfirmAll,
                enabled = products.isNotEmpty(),
                trailingIcon = Icons.Default.ShoppingCart
            )
            Spacer(Modifier.height(8.dp))
            CestouButton(
                text = stringResource(R.string.product_action_retake),
                onClick = onRetake,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalysisResultItem(
    product: Product,
    onUpdate: (Product) -> Unit,
    onRemove: () -> Unit
) {
    CestouCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = product.name,
                    onValueChange = { onUpdate(product.copy(name = it)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.product_photo_item_remove),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = product.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.product_photo_quantity_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                val newQuantity = (product.quantity - QUANTITY_STEP).coerceAtLeast(MIN_QUANTITY)
                                onUpdate(product.copy(quantity = newQuantity))
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        Text(
                            text = product.quantity.formatQuantity(),
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = { onUpdate(product.copy(quantity = product.quantity + QUANTITY_STEP)) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

private val previewProducts = listOf(
    Product(
        id = "1", name = "Tomate",
        quantity = 3.0,
        price = 0.0,
        category = "Hortifruti",
    ),
    Product(
        id = "2",
        name = "Leite Integral",
        quantity = 1.0,
        price = 0.0,
        category = "Laticínios",
    )
)

@Preview(showBackground = true)
@Composable
private fun ProductAnalysisConfirmationPreview() {
    ProductAnalysisConfirmation(
        products = previewProducts,
        onUpdateItem = {},
        onRemoveItem = {},
        onConfirmAll = {},
        onRetake = {}
    )
}
