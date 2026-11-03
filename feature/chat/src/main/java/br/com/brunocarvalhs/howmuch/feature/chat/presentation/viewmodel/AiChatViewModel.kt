package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.AiChatAnalytics
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.VoiceFailure
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.time.TimeSource

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val assistantUseCase: CartAssistantUseCase,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val aiTrialRepository: AiTrialRepository,
    private val aiSession: AiSession,
    private val speechToTextManager: SpeechToTextManager,
    private val firstTimeEventTracker: FirstTimeEventTracker,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    /** Verdadeiro quando o texto a enviar veio do reconhecimento de voz, para o `input_method`. */
    private var inputFromVoice = false

    private val telemetry = AiChatAnalytics(analyticsTracker, firstTimeEventTracker)

    init {
        observeSpeechRecognizer()
    }

    val intent = AiChatIntent(
        onInputChange = { text -> _uiState.update { it.copy(input = text) } },
        onSendMessage = { sendMessage() },
        onToggleHistory = { toggleHistory() },
        onSelectHistoryItem = { id -> selectHistoryItem(id) },
        onStartVoiceInput = { startVoiceInput() },
        onStopVoiceInput = { stopVoiceInput() },
        onStopVoiceInputAndSend = { stopVoiceInputAndSend() },
        onVoiceResultReceived = { text -> handleVoiceResult(text) },
    )

    fun setShoppingContext(shoppingId: String) {
        if (_uiState.value.shoppingId == shoppingId) return
        aiSession.clearHistory()
        _uiState.update { it.copy(shoppingId = shoppingId) }

        viewModelScope.launch {
            val history = chatHistoryRepository.load(shoppingId)
            _uiState.update { it.copy(messages = history) }
        }
    }

    fun loadSession(sessionId: String) {
        aiSession.clearHistory()
        _uiState.update { it.copy(sessionId = sessionId) }

        viewModelScope.launch {
            val history = chatHistoryRepository.loadSession(sessionId)
            _uiState.update { it.copy(messages = history) }
        }
    }

    private fun toggleHistory() {
        telemetry.historyToggled()
        _uiState.update { state ->
            val newShow = !state.showHistory
            state.copy(showHistory = newShow)
        }
    }

    private fun selectHistoryItem(id: String) {
        analyticsTracker.trackAction(AnalyticsElements.AI_CHAT_SELECT_HISTORY)
        _uiState.update { it.copy(showHistory = false) }
        loadSession(id)
    }

    private fun observeSpeechRecognizer() {
        viewModelScope.launch {
            speechToTextManager.isListening.collect { listening ->
                _uiState.update { it.copy(isRecording = listening) }
            }
        }
        viewModelScope.launch {
            speechToTextManager.recognizedText.collect { text ->
                if (!text.isNullOrBlank()) {
                    handleVoiceResult(text)
                }
            }
        }
        viewModelScope.launch {
            speechToTextManager.errorMessage.collect { error ->
                if (!error.isNullOrBlank()) {
                    telemetry.voiceFailed(VoiceFailure.RECOGNITION)
                    _uiState.update { it.copy(voiceErrorMessage = error, isRecording = false) }
                }
            }
        }
    }

    private fun startVoiceInput() {
        telemetry.voiceStarted()
        if (speechToTextManager.isRecognitionAvailable()) {
            _uiState.update { it.copy(isRecording = true, voiceErrorMessage = null) }
            speechToTextManager.clearResult()
            speechToTextManager.startListening()
        } else {
            telemetry.voiceFailed(VoiceFailure.UNSUPPORTED)
            _uiState.update {
                it.copy(
                    voiceErrorMessage = context.getString(R.string.ai_chat_voice_not_supported),
                    isRecording = false,
                )
            }
        }
    }

    private fun stopVoiceInput() {
        speechToTextManager.stopListening()
        _uiState.update { it.copy(isRecording = false) }
    }

    private fun stopVoiceInputAndSend() {
        speechToTextManager.stopListening()
        _uiState.update { it.copy(isRecording = false) }
        val text = _uiState.value.input
        if (text.isNotBlank()) {
            sendMessage()
        }
    }

    private fun handleVoiceResult(text: String) {
        inputFromVoice = true
        _uiState.update { it.copy(input = text) }
    }

    private fun sendMessage() {
        val text = _uiState.value.input
        // Guards against a second concurrent send while the assistant is still answering the
        // first one — the send/mic buttons already disable on isLoading, this is the ViewModel-
        // side backstop (sdd-017, B-06).
        if (text.isBlank() || _uiState.value.isLoading) return

        telemetry.messageSent(text.length, inputFromVoice, !_uiState.value.shoppingId.isNullOrBlank())
        inputFromVoice = false
        val sentAt = TimeSource.Monotonic.markNow()
        aiSession.fallbackUsedThisTurn = false

        val userMessage = ChatMessage(text = text, sender = ChatMessage.Sender.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isLoading = true,
            )
        }
        persistHistory()

        viewModelScope.launch {
            // Tracks whether the trial credit should be spent: never before a response actually
            // comes back, so a network failure no longer burns the user's one free message
            // (sdd-017, B-05).
            var receivedAny = false
            try {
                assistantUseCase(text, _uiState.value)
                    // Guarantees isLoading always returns to false, even if the assistant Flow
                    // completes without emitting anything (sdd-017, C-02).
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
                    .collect { response ->
                        if (!receivedAny) telemetry.responseReceived(sentAt, response.length, aiSession)
                        receivedAny = true
                        val assistantMessage = ChatMessage(text = response, sender = ChatMessage.Sender.ASSISTANT)
                        _uiState.update {
                            it.copy(
                                messages = it.messages + assistantMessage,
                                isLoading = false,
                            )
                        }
                        persistHistory()
                    }
                if (receivedAny) {
                    aiTrialRepository.markFreeMessageUsed()
                    telemetry.trialMessageUsed()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                telemetry.responseFailed(e)
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
        viewModelScope.launch {
            val sessionId = state.sessionId
            if (sessionId != null) {
                chatHistoryRepository.saveSession(
                    sessionId = sessionId,
                    messages = state.messages,
                    shoppingId = state.shoppingId,
                )
            } else {
                val shoppingId = state.shoppingId ?: return@launch
                chatHistoryRepository.save(shoppingId, state.messages)
            }
        }
    }
}
