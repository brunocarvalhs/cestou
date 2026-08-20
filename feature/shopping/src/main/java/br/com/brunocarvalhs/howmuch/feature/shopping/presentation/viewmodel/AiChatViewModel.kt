package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.AiChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AiChatViewModel @Inject constructor(
    private val assistantUseCase: CartAssistantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    val intent = AiChatIntent(
        onInputChange = { text -> _uiState.update { it.copy(input = text) } },
        onSendMessage = { sendMessage() }
    )

    private fun sendMessage() {
        val text = _uiState.value.input
        if (text.isBlank()) return

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
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
