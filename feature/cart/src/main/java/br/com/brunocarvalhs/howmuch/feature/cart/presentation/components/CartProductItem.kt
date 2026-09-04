package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.ui.components.UserAvatar
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency

@Composable
internal fun CartProductItem(
    modifier: Modifier = Modifier,
    name: String = "Arroz",
    quantity: Int = 1,
    price: Double = 10.0,
    total: Double = 10.0,
    isPurchased: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    // Hidden when the list has a single member (spec IAA-01 AC6).
    showAttribution: Boolean = false,
    attributionProfile: UserProfile? = null,
    onShowHistory: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = isPurchased,
                onCheckedChange = { onCheckedChange.invoke(it) }
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val currency = LocalCurrency.current
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "$quantity un • ${price.formatPrice(currency)} cada",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = total.formatPrice(LocalCurrency.current),
                style = MaterialTheme.typography.titleMedium
            )

            if (showAttribution) {
                Spacer(Modifier.width(8.dp))
                // Own clickable region — consumes the tap so it doesn't reach the Card/Checkbox
                // gestures around it.
                UserAvatar(
                    profile = attributionProfile,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onShowHistory)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CartProductItemPreview() {
    CartProductItem()
}

@Preview(name = "Attribution avatar visible (2+ members)")
@Composable
private fun CartProductItemAttributionVisiblePreview() {
    CartProductItem(
        showAttribution = true,
        attributionProfile = UserProfile(id = "user-1", name = "Bruno Carvalhos")
    )
}
