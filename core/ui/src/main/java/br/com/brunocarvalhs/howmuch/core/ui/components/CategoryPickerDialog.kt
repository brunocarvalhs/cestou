package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.brunocarvalhs.howmuch.core.ui.R
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory

private const val DIALOG_MAX_WIDTH_FRACTION = 0.92f
private const val DIALOG_MAX_HEIGHT = 420

/**
 * A single shared category picker for both the Add Product and Edit Product forms (feature/products
 * and feature/cart) — lives in core/ui, alongside [ProductCategory] and [CestouCategoryHeader],
 * so both features render an identical list/icons/checkmark instead of drifting apart.
 *
 * Rendered as a plain [Dialog], not a bottom sheet: callers can push it as its own navigation-graph
 * dialog destination (see ProductScreen's NavHost) or just toggle it from local state (see
 * EditItemContent) — either way it looks and behaves the same to the user.
 */
@Composable
fun CategoryPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(DIALOG_MAX_WIDTH_FRACTION)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.category_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = DIALOG_MAX_HEIGHT.dp)) {
                    items(ProductCategory.entries) { category ->
                        val label = stringResource(category.displayNameRes)
                        val isSelected = label == selected

                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(category.color.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = category.color
                                    )
                                }
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(label) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryPickerDialogPreview() {
    CategoryPickerDialog(
        selected = "Mercearia",
        onSelect = {},
        onDismiss = {}
    )
}
