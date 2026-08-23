package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouSoftGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextPrimary
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextSecondary
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartDetailHeader(
    modifier: Modifier = Modifier,
    iconUrl: String? = null,
    title: String = "",
    description: String = "Compartilhada com João",
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        title = {
            Row {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF0F0F0)),
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
                            else -> "🛒" to Color(0xFFF0F0F0)
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
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CartDetailHeaderPreview() {
    CartDetailHeader(title = "Compras da Semana")
}
