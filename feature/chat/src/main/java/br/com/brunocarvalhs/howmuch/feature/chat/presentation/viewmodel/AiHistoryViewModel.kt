package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiHistoryIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiHistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiHistoryViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    val intent = AiHistoryIntent(
        onSelectSession = { sessionId -> selectSession(sessionId) },
        onDeleteSession = { sessionId -> deleteSession(sessionId) },
        onNewChat = { createNewChat() },
    )

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val sessions = chatHistoryRepository.getSessions()
            _uiState.update { it.copy(sessions = sessions, isLoading = false) }
        }
    }

    private fun selectSession(sessionId: String) {
        analyticsTracker.trackEvent(AnalyticsEvents.AI_SESSION_SELECTED)
        _uiState.update { it.copy(selectedSessionId = sessionId) }
    }

    private fun deleteSession(sessionId: String) {
        analyticsTracker.trackAction(AnalyticsElements.AI_HISTORY_DELETE_SESSION)
        viewModelScope.launch {
            chatHistoryRepository.deleteSession(sessionId)
            loadSessions()
        }
    }

    private fun createNewChat() {
        viewModelScope.launch {
            val newSession = chatHistoryRepository.createNewSession()
            analyticsTracker.trackEvent(AnalyticsEvents.AI_SESSION_CREATED)
            selectSession(newSession.id)
        }
    }
}
