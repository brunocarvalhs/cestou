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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingBasket
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.CommonProductIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.CommonProductUiState

@Composable
internal fun CommonProductForm(
    modifier: Modifier = Modifier,
    uiState: CommonProductUiState,
    intent: CommonProductIntent = CommonProductIntent(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            intent.onMessageShown()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            Text(
                text = stringResource(R.string.common_products_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.common_products_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            CestouButton(
                text = stringResource(R.string.common_products_add_all),
                onClick = { intent.onAddAllToShopping() },
                enabled = uiState.items.isNotEmpty(),
                trailingIcon = Icons.Default.AddShoppingCart
            )
        }

        if (uiState.isLoading && uiState.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.items.isEmpty()) {
            CommonProductEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = uiState.items, key = { it.id }) { item ->
                    CommonProductRow(
                        item = item,
                        onAddToShopping = { intent.onAddToShopping(item) },
                        onRemove = { intent.onRemoveItem(item.id) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.newItemName,
                onValueChange = { intent.onNewItemNameChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.common_products_add_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = { intent.onAddItem() },
                enabled = uiState.newItemName.isNotBlank()
            ) {
                Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = stringResource(R.string.common_products_add_item_description)
                )
            }
        }
    }
}

@Composable
private fun CommonProductRow(
    item: CommonProduct,
    onAddToShopping: () -> Unit,
    onRemove: () -> Unit
) {
    CestouCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${item.category} · ${item.unit}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            IconButton(onClick = onAddToShopping) {
                Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = stringResource(R.string.common_products_item_add_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.common_products_item_remove_description),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CommonProductEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingBasket,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.common_products_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.common_products_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private val previewItems = listOf(
    CommonProduct(id = "1", name = "Arroz", category = "Mercearia", unit = "kg"),
    CommonProduct(id = "2", name = "Feijão", category = "Mercearia", unit = "kg"),
    CommonProduct(id = "3", name = "Leite", category = "Laticínios", unit = "L")
)

@Preview(showBackground = true)
@Composable
private fun CommonProductFormPreview() {
    CommonProductForm(uiState = CommonProductUiState(items = previewItems))
}

@Preview(showBackground = true)
@Composable
private fun CommonProductFormEmptyPreview() {
    CommonProductForm(uiState = CommonProductUiState())
}
