package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragTarget
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.products.R

private const val DISABLED_ALPHA = 0.5f
private const val ENABLED_ALPHA = 1.0f
private const val QUANTITY_STEP = 0.5
private const val CARD_ROUNDED_CORNER = 20
private const val ELEVATION_ENABLED = 2
private const val ELEVATION_DISABLED = 0

@Composable
internal fun ProductListItem(
    product: Product,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onQuantityChange: (Double) -> Unit,
    onTogglePurchased: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val currencyFormatter = rememberCurrencyFormatter()
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (product.isPurchased) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "bgColor"
    )

    val contentAlpha = if (product.isPurchased || !enabled) DISABLED_ALPHA else ENABLED_ALPHA

    val stateDescription = if (product.isPurchased) {
        stringResource(R.string.state_description_in_cart)
    } else {
        stringResource(R.string.state_description_not_in_cart)
    }

    DragTarget(dataToDrop = product) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .semantics(mergeDescendants = true) {
                    this.stateDescription = stateDescription
                },
            shape = RoundedCornerShape(CARD_ROUNDED_CORNER.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (product.isPurchased || !enabled) {
                    ELEVATION_DISABLED.dp
                } else {
                    ELEVATION_ENABLED.dp
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        // Faz com que o TalkBack leia o item como um todo
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onTogglePurchased(!product.isPurchased) },
                    enabled = enabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (product.isPurchased) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = stringResource(R.string.product_item_mark_purchased),
                        tint = if (product.isPurchased) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (product.isPurchased) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            }
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = contentAlpha),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currencyFormatter.format(product.price),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "• ${product.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )

                        Text(
                            text = "x ${product.quantity} ${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currencyFormatter.format(product.total),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha))
                    ) {
                        IconButton(
                            onClick = { onQuantityChange(product.quantity - QUANTITY_STEP) },
                            enabled = enabled,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.content_description_remove_unit),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = if (product.quantity % ENABLED_ALPHA == 0.0) {
                                product.quantity.toInt().toString()
                            } else {
                                product.quantity.toString()
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { onQuantityChange(product.quantity + QUANTITY_STEP) },
                            enabled = enabled,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.content_description_add_unit),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        enabled = enabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.product_item_more_options),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private const val PREVIEW_BACKGROUND_COLOR = 0xFFF5F5F5

@Preview(showBackground = true)
@Composable
private fun ProductListItemPreview() {
    MaterialTheme {
        Column(Modifier.background(Color(PREVIEW_BACKGROUND_COLOR))) {
            ProductListItem(
                product = Product(
                    id = "1",
                    name = "Cerveja Heineken 600ml",
                    quantity = 6.0,
                    price = 9.90,
                    isPurchased = false
                ),
                onDelete = {},
                onEdit = {},
                onQuantityChange = {},
                onTogglePurchased = {}
            )
            ProductListItem(
                product = Product(
                    id = "1",
                    name = "Pão de Forma Integral",
                    quantity = 1.0,
                    price = 12.50,
                    isPurchased = true
                ),
                onDelete = {},
                onEdit = {},
                onQuantityChange = {},
                onTogglePurchased = {}
            )
        }
    }
}
