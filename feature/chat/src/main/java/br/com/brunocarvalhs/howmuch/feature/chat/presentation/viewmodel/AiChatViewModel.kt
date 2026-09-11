package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val assistantUseCase: CartAssistantUseCase,
    private val analyticsTracker: AnalyticsTracker,
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
        _uiState.update { it.copy(shoppingId = shoppingId) }
    }

    private fun sendMessage() {
        val text = _uiState.value.input
        if (text.isBlank()) return

        analyticsTracker.trackEvent(AnalyticsEvents.AI_CHAT_MESSAGE_SENT)

        val userMessage = ChatMessage(text = text, sender = ChatMessage.Sender.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isLoading = true
            )
        }

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
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Só chega aqui quando todos os providers de IA falharam (ex.: desligados
                // remotamente via feature flag) — o usuário precisa ver algo, não só o
                // indicador de carregamento sumir em silêncio.
                val errorMessage = ChatMessage(
                    text = context.getString(R.string.ai_chat_error_message),
                    sender = ChatMessage.Sender.ASSISTANT
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isLoading = false
                    )
                }
            }
        }
    }
}
