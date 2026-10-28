package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val assistantUseCase: CartAssistantUseCase,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val aiTrialRepository: AiTrialRepository,
    private val aiSession: AiSession,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        analyticsTracker.trackScreenView(screenName = "ai_chat", screenClass = "AiChatViewModel")
    }

    val intent = AiChatIntent(
        onInputChange = { text -> _uiState.update { it.copy(input = text) } },
        onSendMessage = { sendMessage() }
    )

    fun setShoppingContext(shoppingId: String) {
        if (_uiState.value.shoppingId == shoppingId) return
        // AiSession.history é compartilhado entre listas (é @Singleton) — sem isto, o contexto
        // da conversa de uma lista vazava para a próxima lista aberta (sdd-006, B-11).
        aiSession.clearHistory()
        _uiState.update { it.copy(shoppingId = shoppingId) }

        viewModelScope.launch {
            val history = chatHistoryRepository.load(shoppingId)
            _uiState.update { it.copy(messages = history) }
        }
    }

    private fun sendMessage() {
        val text = _uiState.value.input
        if (text.isBlank()) return

        analyticsTracker.trackEvent(AnalyticsEvents.AI_CHAT_MESSAGE_SENT)
        viewModelScope.launch { aiTrialRepository.markFreeMessageUsed() }

        val userMessage = ChatMessage(text = text, sender = ChatMessage.Sender.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isLoading = true
            )
        }
        persistHistory()

        viewModelScope.launch {
            try {
                assistantUseCase(text, _uiState.value).collect { response ->
                    val assistantMessage = ChatMessage(text = response, sender = ChatMessage.Sender.ASSISTANT)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                    persistHistory()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Último ponto da cadeia de agentes (GeminiAiAgent/OpenRouterAiAgent -> opcional
                // FallbackAiAgent -> aqui): depois do fix de B-08, uma falha de transporte sem
                // nenhum provider restante propaga até aqui em vez de já chegar como texto
                // (sdd-006). Antes, esse catch só escondia o spinner, sem nenhuma mensagem.
                val errorText = if (e is IOException) {
                    context.getString(R.string.ai_chat_error_network)
                } else {
                    context.getString(R.string.ai_chat_error_generic)
                }
                val errorMessage = ChatMessage(text = errorText, sender = ChatMessage.Sender.ASSISTANT)
                _uiState.update {
                    it.copy(messages = it.messages + errorMessage, isLoading = false)
                }
                persistHistory()
            }
        }
    }

    private fun persistHistory() {
        val state = _uiState.value
        val shoppingId = state.shoppingId ?: return
        viewModelScope.launch {
            chatHistoryRepository.save(shoppingId, state.messages)
        }
    }
}
