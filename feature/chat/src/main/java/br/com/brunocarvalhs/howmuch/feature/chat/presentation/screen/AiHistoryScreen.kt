package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiHistoryIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiHistoryUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatSessionDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHistoryScreen(
    state: AiHistoryUiState,
    intent: AiHistoryIntent,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.ai_chat_history_title)) },
                navigationIcon = {
                    IconButton(onClick = intent.onBack, modifier = Modifier.testTag("ai_history_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ai_chat_close_content_description),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = intent.onNewChat,
                modifier = Modifier.testTag("ai_history_new_chat"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.ai_chat_history_new_chat),
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.sessions.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.Center).testTag("ai_history_empty"),
                    text = stringResource(R.string.ai_chat_history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.sessions,
                        key = { it.id },
                    ) { session ->
                        ChatSessionCard(
                            session = session,
                            onClick = { intent.onSelectSession(session.id) },
                            onDelete = { intent.onDeleteSession(session.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSessionCard(
    session: ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_history_session")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (session.lastMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatSessionDate(session.updatedAtEpochMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.testTag("ai_history_session_delete")) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.ai_chat_history_delete_content_description),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
