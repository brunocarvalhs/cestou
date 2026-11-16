package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.extensions.orEmpty
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.ui.components.UserAvatar
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatQuantity
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductListItem(
    modifier: Modifier = Modifier,
    product: Product,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onTogglePurchased: (Boolean) -> Unit = { },
    enabled: Boolean = true,
    // Hidden when the list has a single member (spec IAA-01 AC6) — the caller decides visibility
    // by only passing a non-null profile lookup when `shopping.users.size > 1`.
    showAttribution: Boolean = false,
    attributionProfile: UserProfile? = null,
    onShowHistory: () -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    )

    SwipeToDismissBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("cart_product_item"),
        state = dismissState,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = enabled,
        onDismiss = { swipe ->
            if (enabled && swipe != SwipeToDismissBoxValue.Settled) {
                onDelete()
            }
        },
        backgroundContent = { DeleteSwipeBackground(direction = dismissState.dismissDirection) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .combinedClickable(
                    enabled = enabled,
                    onClick = { onTogglePurchased(!product.isPurchased) },
                    onLongClick = { onEdit.invoke() }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PurchaseIndicator(
                checked = product.isPurchased,
                enabled = enabled,
                onToggle = { onTogglePurchased(!product.isPurchased) },
                modifier = Modifier.testTag("cart_product_checkbox")
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (product.isPurchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (product.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = product.quantity.formatQuantity(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (product.price.orEmpty() > 0.0) {
                Text(
                    text = currencyFormatter().format(product.total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showAttribution) {
                Spacer(modifier = Modifier.width(8.dp))
                // Own clickable, nested inside the row's own Modifier chain — it consumes the
                // tap before it reaches the Column's combinedClickable above, so it can't be
                // mistaken for the purchased-toggle/edit gestures on the rest of the row.
                val historyDescription =
                    stringResource(CoreUiR.string.content_description_product_history)
                UserAvatar(
                    profile = attributionProfile,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            enabled = enabled,
                            onClick = onShowHistory
                        )
                        .semantics {
                            contentDescription = historyDescription
                        }
                )
            }
        }
    }
}

@Composable
private fun currencyFormatter() = rememberCurrencyFormatter()

/**
 * Marcador quadrado de 24 dp do canvas da nova identidade: preenchido em azul quando o item
 * está comprado. Mantém o papel de checkbox para a acessibilidade.
 */
@Composable
private fun PurchaseIndicator(
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(2.dp, tint, shape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteSwipeBackground(direction: SwipeToDismissBoxValue) {
    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.cart_item_delete_description),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Preview
@Composable
private fun ProductListItemPreview() {
    ProductListItem(
        product = Product(
            id = "",
            name = "Arroz",
            quantity = 2.0,
            price = 2.50
        ),
    )
}

@Preview
@Composable
private fun ProductListItemEmptyPricePreview() {
    ProductListItem(
        product = Product(
            id = "",
            name = "Arroz",
            quantity = 2.0,
        ),
    )
}

@Preview
@Composable
private fun ProductListItemLockedPreview() {
    ProductListItem(
        product = Product(
            id = "",
            name = "Arroz",
            quantity = 2.0,
        ),
    )
}

@Preview(name = "Attribution avatar visible (2+ members)")
@Composable
private fun ProductListItemAttributionVisiblePreview() {
    ProductListItem(
        product = Product(
            id = "",
            name = "Arroz",
            quantity = 2.0,
            price = 2.50
        ).withActivity(ProductActivity.Action.ADDED, "user-1"),
        showAttribution = true,
        attributionProfile = UserProfile(id = "user-1", name = "Bruno Carvalhos")
    )
}

@Preview(name = "Attribution avatar hidden (single member)")
@Composable
private fun ProductListItemAttributionHiddenPreview() {
    ProductListItem(
        product = Product(
            id = "",
            name = "Arroz",
            quantity = 2.0,
            price = 2.50
        ).withActivity(ProductActivity.Action.ADDED, "user-1"),
        showAttribution = false
    )
}
