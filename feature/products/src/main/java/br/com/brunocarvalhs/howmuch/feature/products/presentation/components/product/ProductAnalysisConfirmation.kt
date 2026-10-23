package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatQuantity
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

private const val QUANTITY_STEP = 1.0
private const val MIN_QUANTITY = 1.0

// RoundedCornerShape percent (0-100): 50 yields a fully rounded "pill" shape.
private const val STEPPER_PILL_CORNER_PERCENT = 50

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
    val category = remember(product.category) { ProductCategory.fromString(product.category) }

    CestouCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = category.color.copy(alpha = 0.18f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        // Decorativo: a categoria já é lida como texto no subtítulo abaixo.
                        contentDescription = null,
                        tint = category.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = product.name,
                            onValueChange = { onUpdate(product.copy(name = it)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            // Decorativo: só reforça que o nome ao lado é editável.
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(category.displayNameRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = category.color
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.product_photo_item_remove),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                QuantityStepper(
                    quantity = product.quantity,
                    onDecrease = {
                        val newQuantity = (product.quantity - QUANTITY_STEP).coerceAtLeast(MIN_QUANTITY)
                        onUpdate(product.copy(quantity = newQuantity))
                    },
                    onIncrease = { onUpdate(product.copy(quantity = product.quantity + QUANTITY_STEP)) },
                    decreaseEnabled = product.quantity > MIN_QUANTITY
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean
) {
    val quantityLabel = stringResource(R.string.product_photo_quantity_label)
    val formattedQuantity = quantity.formatQuantity()

    Surface(
        modifier = Modifier.semantics {
            contentDescription = "$quantityLabel $formattedQuantity"
        },
        shape = RoundedCornerShape(STEPPER_PILL_CORNER_PERCENT),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, enabled = decreaseEnabled) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(CoreR.string.content_description_remove_unit),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = formattedQuantity,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 28.dp)
            )
            IconButton(onClick = onIncrease) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(CoreR.string.content_description_add_unit),
                    modifier = Modifier.size(18.dp)
                )
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
    ),
    Product(
        id = "3",
        name = "Detergente Neutro Concentrado 500ml",
        quantity = 2.0,
        price = 0.0,
        category = "Limpeza",
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

@Preview(showBackground = true, name = "Item único")
@Composable
private fun AnalysisResultItemPreview() {
    AnalysisResultItem(
        product = previewProducts.first(),
        onUpdate = {},
        onRemove = {}
    )
}

@Preview(showBackground = true, name = "Quantidade mínima (botão - desabilitado)")
@Composable
private fun AnalysisResultItemMinQuantityPreview() {
    AnalysisResultItem(
        product = previewProducts[1],
        onUpdate = {},
        onRemove = {}
    )
}
