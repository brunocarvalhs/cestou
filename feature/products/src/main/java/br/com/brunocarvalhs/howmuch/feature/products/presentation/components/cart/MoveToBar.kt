package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DropTarget
import br.com.brunocarvalhs.howmuch.feature.products.R

private const val BORDER_ALPHA = 0.3f
private val ROUNDED_CORNER_LARGE = 24.dp
private val ROUNDED_CORNER_MEDIUM = 16.dp
private val ICON_SIZE = 18.dp

@Composable
internal fun MoveToBar(
    visible: Boolean,
    shoppings: List<Shopping>,
    onMove: (Any, String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ROUNDED_CORNER_LARGE),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.shopping_list_move_to),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(shoppings) { shopping ->
                        DropTarget(
                            onDataDropped = { data -> onMove(data, shopping.id) }
                        ) { isHovered, _ ->
                            val containerColor by animateColorAsState(
                                targetValue = if (isHovered) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                label = "dropColor"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isHovered) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                label = "dropContentColor"
                            )

                            Surface(
                                shape = RoundedCornerShape(ROUNDED_CORNER_MEDIUM),
                                color = containerColor,
                                border = if (!isHovered) {
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = BORDER_ALPHA)
                                    )
                                } else {
                                    null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        modifier = Modifier.size(ICON_SIZE),
                                        tint = contentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = shopping.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoveToBarPreview() {
    MaterialTheme {
        MoveToBar(
            visible = true,
            shoppings = listOf(
                Shopping(
                    id = "1",
                    title = "Compras Casa",
                    description = "",
                    price = 0.0,
                    status = Shopping.Status.NEW,
                    users = emptyList(),
                    roles = emptyMap()
                ),
                Shopping(
                    id = "2",
                    title = "Churrasco",
                    description = "",
                    price = 0.0,
                    status = Shopping.Status.NEW,
                    users = emptyList(),
                    roles = emptyMap()
                )
            ),
            onMove = { _, _ -> }
        )
    }
}
