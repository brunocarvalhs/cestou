package br.com.brunocarvalhs.howmuch.feature.chat.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession

@Stable
data class AiHistoryUiState(
    val sessions: List<ChatSession> = emptyList(),
    val isLoading: Boolean = false,
    val selectedSessionId: String? = null
)
