package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.rememberPlaceholderState
import br.com.brunocarvalhs.data.model.ProductModel
import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString

@Composable
fun ShoppingCartItem(
    product: Product? = null,
    onRemove: (() -> Unit)? = null,
    onQuantityChange: ((Int) -> Unit)? = null,
    isLoading: Boolean = false,
    isExpanded: Boolean = false,
    titleFillMaxWidth: Float = 0.6f,
    priceFillMaxWidth: Float = 0.4f,
    quantityFillMaxWidth: Float = 0.3f,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    val placeholderState = rememberPlaceholderState(isVisible = isLoading)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(titleFillMaxWidth)
                                .height(20.dp)
                                .placeholder(
                                    placeholderState = placeholderState,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(priceFillMaxWidth)
                                .height(16.dp)
                                .placeholder(
                                    placeholderState = placeholderState,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(quantityFillMaxWidth)
                                .height(14.dp)
                                .placeholder(
                                    placeholderState = placeholderState,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    } else {
                        Row {
                            onCheckedChange?.let {
                                Checkbox(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .semantics {
                                            testTagsAsResourceId = true
                                        }
                                        .testTag("check_product"),
                                    checked = product?.isChecked ?: false,
                                    onCheckedChange = { onCheckedChange.invoke(it) }
                                )
                            }
                            Column {
                                if (product?.name.isNullOrBlank().not()) {
                                    Text(
                                        product.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                product?.price?.let {
                                    Text(
                                        text = stringResource(R.string.price) + " " + stringResource(
                                            R.string.currency,
                                            product.price?.toCurrencyString().orEmpty()
                                        ),
                                        style = if (product.name.isBlank()) {
                                            MaterialTheme.typography.titleMedium
                                        } else {
                                            MaterialTheme.typography.bodyMedium
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (onRemove == null || onQuantityChange == null) {
                                    Text(
                                        stringResource(
                                            R.string.quantity,
                                            product?.quantity ?: ONE_INT
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    AnimatedVisibility(
                                        expanded.not()
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.quantity,
                                                product?.quantity ?: ONE_INT
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isLoading) {
                    if (onRemove != null || onQuantityChange != null) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = if (expanded) {
                                    stringResource(R.string.close_quantity_controls)
                                } else {
                                    stringResource(
                                        R.string.open_quantity_controls
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .placeholder(
                                placeholderState = placeholderState,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            if (onRemove != null || onQuantityChange != null) {
                AnimatedVisibility(
                    expanded
                ) {
                    Row {
                        onRemove?.let {
                            IconButton(
                                onClick = { onRemove() },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .testTag("delete_product")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_selected)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        onQuantityChange?.let {
                            QuantityControls(
                                onQuantityChange = onQuantityChange,
                                product = product
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityControls(
    onQuantityChange: (Int) -> Unit,
    product: Product? = null
) {
    product?.let { prod ->
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.testTag("decrease_quantity"),
                enabled = product.quantity > ONE_INT,
                onClick = {
                    if (product.quantity > ONE_INT) {
                        onQuantityChange.invoke(product.quantity - ONE_INT)
                    }
                }
            ) {
                Icon(
                    painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.decrease_quantity)
                )
            }

            Text(
                text = prod.quantity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(
                modifier = Modifier.testTag("increase_quantity"),
                onClick = { onQuantityChange.invoke(prod.quantity + ONE_INT) }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.increase_quantity)
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartItemPreview() {
    ShoppingCartItem(
        product = ProductModel(
            name = "Produto de Teste",
            quantity = 1,
            price = 1000
        ),
        onRemove = {},
        onQuantityChange = { },
        isLoading = false
    )
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartItemWithoutProductPreview() {
    ShoppingCartItem(
        product = ProductModel(
            quantity = 1,
            price = 1000
        ),
    )
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartItemLoadingPreview() {
    ShoppingCartItem(
        isLoading = true
    )
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartItemWithIsExpandedPreview() {
    ShoppingCartItem(
        product = ProductModel(
            name = "Produto de Teste",
            quantity = 1,
            price = 1000
        ),
        onRemove = {},
        onQuantityChange = { },
        isLoading = false,
        isExpanded = true
    )
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartItemWithoutProductIsExpandedPreview() {
    ShoppingCartItem(
        product = ProductModel(
            quantity = 1,
            price = 1000
        ),
        isExpanded = true
    )
}

@Composable
@Preview(showBackground = true)
private fun ShoppingCartItemWithoutProductCheckedPreview() {
    ShoppingCartItem(
        product = ProductModel(
            name = "Produto de Teste",
            quantity = 1,
        ),
        isExpanded = true,
        onCheckedChange = { }
    )
}
