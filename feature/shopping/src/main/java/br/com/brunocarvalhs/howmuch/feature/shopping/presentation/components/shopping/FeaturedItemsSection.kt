package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCheckRow
import br.com.brunocarvalhs.howmuch.core.ui.components.UserAvatars
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.shopping.R

private const val MAX_VISIBLE_ITEMS = 3

/**
 * Seção "Itens da vez" do canvas: até três itens da lista em destaque, o contador de marcados e
 * os avatares de quem participa. Tocar em um item abre a lista, onde ele é marcado. Some sem itens.
 */
@Composable
internal fun FeaturedItemsSection(
    items: List<Product>,
    users: List<String>,
    currencyFormatter: CurrencyFormatter,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val checked = items.count { it.isPurchased }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.shopping_featured_items_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.shopping_featured_items_checked, checked, items.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items.take(MAX_VISIBLE_ITEMS).forEach { product ->
            CestouCheckRow(
                title = product.name,
                price = product.price?.let { currencyFormatter.format(product.total) },
                checked = product.isPurchased,
                onCheckedChange = { onOpen() }
            )
        }

        if (users.isNotEmpty()) {
            UserAvatars(profiles = List<UserProfile?>(users.size) { null })
        }
    }
}
