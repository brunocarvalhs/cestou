package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import coil.compose.AsyncImage

@Composable
internal fun ShoppingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    price: Double,
    itemCount: Int = 0,
    users: List<String> = emptyList(),
    status: Shopping.Status = Shopping.Status.NEW,
    iconUrl: String? = null,
    iconBackgroundColor: Color = Color(0xFFF0F0F0),
    onClick: () -> Unit = {},
    // Parametros mantidos para compatibilidade
    onEditClick: () -> Unit = {},
    onDuplicateClick: () -> Unit = {},
    onFinishClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    budget: Double? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                val (emoji, bgColor) = when {
                    title.contains("breakfast", true) || title.contains("café", true) -> "🍳" to Color(0xFFFEF9E7)
                    title.contains("dinner", true) || title.contains("jantar", true) -> "🍏" to Color(0xFFE8F8F5)
                    title.contains("pizza", true) -> "🍕" to Color(0xFFFEF5E7)
                    title.contains("spaghetti", true) || title.contains("massa", true) -> "🍝" to Color(0xFFFBEEE6)
                    title.contains("drinks", true) || title.contains("bebidas", true) -> "🍺" to Color(0xFFF5EEF8)
                    title.contains("purchased", true) -> "🍉" to Color(0xFFFCE4EC)
                    else -> "🛒" to iconBackgroundColor
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$itemCount products",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (users.size > 1) {
                UserAvatars(users = users)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.LightGray
            )
        }
    }
}

@Composable
private fun UserAvatars(users: List<String>) {
    val displayCount = users.take(2)
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        displayCount.forEach { _ ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ShoppingItemLoading(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingItemPreview() {
    CestouTheme {
        Column {
            ShoppingItem(
                title = "Morning breakfast",
                description = "",
                price = 0.0,
                itemCount = 8,
                users = listOf("1", "2")
            )
            ShoppingItem(
                title = "Pizza day!",
                description = "",
                price = 0.0,
                itemCount = 4
            )
            ShoppingItem(
                title = "Often purchased",
                description = "",
                price = 0.0,
                itemCount = 10,
                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingItemLoadingPreview() {
    CestouTheme {
        ShoppingItemLoading()
    }
}
