package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.components.shopping

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouSoftGreen
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouSoftOrange
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTextPrimary
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTextSecondary
import br.com.brunocarvalhs.howmuch.feature.shopping.R

@Composable
internal fun ShoppingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    price: Double,
    itemCount: Int = 12, // Mocked for design faithfulness
    isShared: Boolean = false,
    users: List<String> = emptyList(),
    status: Shopping.Status = Shopping.Status.NEW,
    onClick: () -> Unit = {},
    // Keeping internal parameters for compatibility with screen
    onEditClick: () -> Unit = {},
    onDuplicateClick: () -> Unit = {},
    onFinishClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    budget: Double? = null
) {
    val backgroundColor = if (title.contains("Churrasco", ignoreCase = true)) {
        CestouSoftOrange
    } else {
        CestouSoftGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = CestouTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isShared) "compartilhada com João" else "Privada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CestouTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$itemCount itens",
                        style = MaterialTheme.typography.labelSmall,
                        color = CestouTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatars(count = if (isShared) 2 else 1)

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CestouTextPrimary
                )
            }
        }
    }
}

@Composable
private fun UserAvatars(count: Int) {
    Row {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
            }
            if (index < count - 1) {
                Spacer(modifier = Modifier.width((-12).dp)) // Overlap
            }
        }
    }
}

@Composable
internal fun ShoppingItemLoading(modifier: Modifier = Modifier) {
    // Basic loading state
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
    )
}
