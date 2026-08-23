package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import coil.compose.AsyncImage

@Composable
internal fun IconProduct(
    product: Product,
    onClick: (Boolean) -> Unit,
    enabled: Boolean,
    iconUrl: String? = null,
    iconBackgroundColor: Color = Color(0xFFF0F0F0),
    iconSelectBackgroundColor: Color = Color(0xFF009688),
) {
    val title = remember { product.name }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) iconSelectBackgroundColor else iconBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onClick(!product.isPurchased) },
            enabled = enabled,
            modifier = Modifier.size(32.dp)
        ) {
            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                val (emoji, bgColor) = when {
                    title.contains("breakfast", true) || title.contains(
                        "café",
                        true
                    ) -> "🍳" to Color(
                        0xFFFEF9E7
                    )

                    title.contains("dinner", true) || title.contains(
                        "jantar",
                        true
                    ) -> "🍏" to Color(
                        0xFFE8F8F5
                    )

                    title.contains("pizza", true) -> "🍕" to Color(0xFFFEF5E7)
                    title.contains("spaghetti", true) || title.contains(
                        "massa",
                        true
                    ) -> "🍝" to Color(
                        0xFFFBEEE6
                    )

                    title.contains("drinks", true) || title.contains(
                        "bebidas",
                        true
                    ) -> "🍺" to Color(
                        0xFFF5EEF8
                    )

                    title.contains("purchased", true) -> "🍉" to Color(0xFFFCE4EC)
                    else -> "🛒" to iconBackgroundColor
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}