package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.NotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.NotificationType
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.NotificationsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsScreen(
    state: NotificationsUiState,
    intent: NotificationsIntent
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notificações",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { intent.onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(state.notifications) { notification ->
                NotificationCard(
                    notification = notification,
                    onClick = { intent.onNotificationClick(notification.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val borderColor = if (!notification.isRead) MaterialTheme.colorScheme.primary else Color.Transparent
    
    CestouCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!notification.isRead) Modifier.border(2.dp, borderColor, MaterialTheme.shapes.large)
                else Modifier
            ),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.ACTION -> Icons.Default.Notifications
                        NotificationType.REMINDER -> Icons.Default.Email
                        NotificationType.FEATURE -> Icons.Default.Email
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = notification.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotificationsPreview() {
    NotificationsScreen(
        state = NotificationsUiState(
            notifications = listOf(
                NotificationItem(
                    id = "1",
                    title = "João completou 3 itens",
                    description = "Leite Integral, Queijo Minas Frescal e Banana Prata foram riscados da lista Compras da Semana.",
                    time = "Há 5 min",
                    type = NotificationType.ACTION,
                    isRead = false
                )
            )
        ),
        intent = NotificationsIntent()
    )
}
