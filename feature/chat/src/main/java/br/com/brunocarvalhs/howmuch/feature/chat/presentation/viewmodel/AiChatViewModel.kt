package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.di.ChatBackgroundScope
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.AiTurnBackground
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.ChatTurnTracker
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.AiChatAnalytics
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.AiChatVoiceInput
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiTurnInfo
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.MessageVote
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.Vote
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val MIN_RESPONSES_FOR_CSAT = 2
private const val PROVIDER_NONE = "none"
private const val SOURCE_CHIP = "chip"
private const val SOURCE_ITEM = "item"

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val assistantUseCase: CartAssistantUseCase,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val aiTrialRepository: AiTrialRepository,
    private val aiSession: AiSession,
    speechToTextManager: SpeechToTextManager,
    private val firstTimeEventTracker: FirstTimeEventTracker,
    private val feedbackPromptRequester: FeedbackPromptRequester,
    @ApplicationContext private val context: Context,
    @ChatBackgroundScope private val backgroundScope: CoroutineScope,
    private val turnTracker: ChatTurnTracker,
    private val background: AiTurnBackground,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    private val telemetry = AiChatAnalytics(analyticsTracker, firstTimeEventTracker)

    private val voice = AiChatVoiceInput(speechToTextManager, telemetry, context, _uiState)

    /**
     * Respostas reais do assistente nesta abertura do chat: o CSAT só vale a partir de 2. Escrito
     * pelo turno em `Dispatchers.Default` e lido na thread principal no `onClose`, por isso volátil;
     * há um único escritor por vez, pois o envio fica bloqueado enquanto `isLoading`.
     */
    @Volatile
    private var assistantResponses = 0

    init {
        voice.observe(viewModelScope)
        viewModelScope.launch {
            aiSession.activeAction.collect { id -> _uiState.update { it.copy(activeAction = id) } }
        }
        viewModelScope.launch {
            aiSession.pendingApproval.collect { request ->
                _uiState.update { it.copy(pendingApproval = request) }
                if (request != null) background.approvalNeeded(_uiState.value.sessionId)
            }
        }
    }

    val intent = AiChatIntent(
        onInputChange = { text -> _uiState.update { it.copy(input = text) } },
        onSendMessage = { sendMessage() },
        onToggleHistory = { toggleHistory() },
        onSelectHistoryItem = { id -> selectHistoryItem(id) },
        onStartVoiceInput = {
            telemetry.voiceStarted()
            voice.start()
        },
        onStopVoiceInput = { voice.stop() },
        onStopVoiceInputAndSend = {
            voice.stop()
            if (_uiState.value.input.isNotBlank()) sendMessage()
        },
        onVoiceResultReceived = { text -> voice.onResult(text) },
        /**
         * Um voto por mensagem na sessão da tela; votar numa resposta sem turno conhecido usa
         * `none`/0. O 👎 sai sem `reason`; o chip repete o evento com ele.
         */
        onMessageVote = { messageId, vote ->
            val state = _uiState.value
            if (state.feedbackEnabled && !state.votes.containsKey(messageId)) {
                _uiState.update { it.copy(votes = it.votes + (messageId to MessageVote(vote))) }
                telemetry.messageVoted(vote, state.turns[messageId])
            }
        },
        onMessageVoteReason = { messageId, reason ->
            val state = _uiState.value
            val current = state.votes[messageId]
            if (current?.value == Vote.DOWN && current.reason == null) {
                _uiState.update { it.copy(votes = it.votes + (messageId to current.copy(reason = reason))) }
                telemetry.messageVoted(Vote.DOWN, state.turns[messageId], reason)
            }
        },
        /** A decisão é reportada pelo agente (`ai_tool_confirmation_resolved`), que também vê tempo esgotado. */
        onApprove = { aiSession.resolveApproval(ApprovalDecision.APPROVED) },
        onReject = { aiSession.resolveApproval(ApprovalDecision.REJECTED) },
        onSuggestionClick = { text ->
            telemetry.suggestionClicked(SOURCE_CHIP)
            sendMessage(text, fromInput = false)
        },
        /** Pedir à IA reaproveita as ferramentas de lista dela, sem o chat depender de `feature:shopping`. */
        onAddItems = { items ->
            if (items.isNotEmpty()) {
                if (items.size == 1) telemetry.suggestionClicked(SOURCE_ITEM) else telemetry.batchItemsAdded(items.size)
                val prompt = context.getString(R.string.ai_chat_add_items_prompt, items.joinToString(", "))
                sendMessage(prompt, fromInput = false)
            }
        },
        /** Fechar o chat é o momento do CSAT; a pesquisa só abre num destino estável. */
        onClose = {
            if (assistantResponses >= MIN_RESPONSES_FOR_CSAT) {
                feedbackPromptRequester.onMoment(FeedbackMoment.AI_CHAT_CLOSED)
            }
        },
    )

    /**
     * Abrir o chat a partir de uma lista sempre começa uma conversa nova: conversas anteriores
     * ficam no histórico e só voltam por [loadSession]. A memória da IA começa vazia.
     */
    /** O chat entrou ou saiu de vista; fora de vista, o fim do pedido vira notificação. */
    val setChatVisible: (Boolean) -> Unit = { visible -> background.setChatVisible(visible) }

    /** Fechar o chat com o card aberto recusa o pedido, para o turno não ficar preso esperando. */
    val dismissPendingApproval: () -> Unit = {
        if (_uiState.value.pendingApproval != null) aiSession.resolveApproval(ApprovalDecision.DISMISSED)
    }

    fun setShoppingContext(shoppingId: String) {
        if (_uiState.value.shoppingId == shoppingId) return
        val othersRunning = turnTracker.inFlight.value.isNotEmpty()
        _uiState.update { it.copy(shoppingId = shoppingId, isLoading = othersRunning) }

        viewModelScope.launch {
            awaitIdleTurns()
            aiSession.clearHistory()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Reabre uma conversa gravada e devolve o que foi dito à memória da IA; sem isso a tela mostra
     * o histórico, mas o assistente responde como se fosse a primeira mensagem.
     */
    fun loadSession(sessionId: String) {
        val turnPending = sessionId in turnTracker.inFlight.value
        val othersRunning = turnTracker.inFlight.value.isNotEmpty()
        _uiState.update { it.copy(sessionId = sessionId, isLoading = othersRunning) }

        viewModelScope.launch {
            if (turnPending) {
                _uiState.update { it.copy(messages = chatHistoryRepository.loadSession(sessionId)) }
                awaitPendingTurn(sessionId) { chatHistoryRepository.loadSession(sessionId) }
                return@launch
            }
            awaitIdleTurns()
            val history = chatHistoryRepository.loadSession(sessionId)
            val shoppingId = chatHistoryRepository.getSessions().firstOrNull { it.id == sessionId }?.shoppingId
            aiSession.restoreHistory(history.map { AiHistoryTurn(it.sender == ChatMessage.Sender.USER, it.text) })
            _uiState.update {
                it.copy(messages = history, shoppingId = it.shoppingId ?: shoppingId, isLoading = false)
            }
        }
    }

    /** O `AiSession` é singleton: só se troca a memória dele sem nenhum turno usando-a. */
    private suspend fun awaitIdleTurns() {
        turnTracker.inFlight.first { it.isEmpty() }
    }

    /**
     * Um turno aberto por uma instância anterior deste chat ainda está respondendo: o `AiSession`
     * singleton não pode ser limpo nem receber um segundo envio. Espera o fim e recarrega o
     * histórico, que já traz a resposta (ou o erro) gravado pelo turno.
     */
    private suspend fun awaitPendingTurn(key: String, reload: suspend () -> List<ChatMessage>) {
        turnTracker.inFlight.first { key !in it }
        val history = reload()
        _uiState.update { it.copy(messages = history, isLoading = false) }
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

    /**
     * [fromInput] é falso para texto que não veio do campo (chips e ações de itens): o rascunho
     * digitado continua lá e a origem por voz não é consumida.
     */
    private fun sendMessage(text: String = _uiState.value.input, fromInput: Boolean = true) {
        // Guards against a second concurrent send while the assistant is still answering the
        // first one — the send/mic buttons already disable on isLoading, this is the ViewModel-
        // side backstop (sdd-017, B-06).
        if (text.isBlank() || _uiState.value.isLoading) return

        val fromVoice = fromInput && voice.consumeInputFromVoice()
        telemetry.messageSent(text.length, fromVoice, !_uiState.value.shoppingId.isNullOrBlank())
        val sentAt = TimeSource.Monotonic.markNow()
        aiSession.fallbackUsedThisTurn = false

        val userMessage = ChatMessage(text = text, sender = ChatMessage.Sender.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = if (fromInput) "" else it.input,
                isLoading = true,
            )
        }
        val key = resolveConversationKey()

        /**
         * Fora do viewModelScope: fechar o chat não pode cancelar a chamada à IA nem perder a
         * resposta. Registrado no rastreador antes de iniciar, para um chat reaberto já enxergá-lo.
         */
        val turn = backgroundScope.launch(start = CoroutineStart.LAZY) { runTurn(key, text, sentAt) }
        turnTracker.begin(key, turn)
        background.turnStarted()
        turn.start()
    }

    /**
     * Um turno completo (gravação da mensagem, chamada à IA e gravação da resposta). Roda no scope
     * de aplicação e só termina por conclusão, falha ou [ChatTurnTracker.cancelAll].
     */
    private suspend fun runTurn(key: String, text: String, sentAt: TimeMark) {
        // Só vira resultado se o turno terminou por conta própria; cancelado (logout) não notifica.
        var result: String? = null
        try {
            persistHistory()
            collectResponse(text, sentAt)
            result = _uiState.value.messages.lastOrNull { it.sender == ChatMessage.Sender.ASSISTANT }?.text
        } finally {
            turnTracker.end(key)
            background.turnFinished(key, result)
        }
    }

    /**
     * Chave da conversa no [ChatTurnTracker]. Todo chat novo ganha uma sessão própria antes do
     * primeiro envio, mesmo vindo de uma lista: gravar pelo `shoppingId` anexaria a conversa a
     * uma sessão antiga da mesma lista.
     */
    private fun resolveConversationKey(): String {
        _uiState.value.sessionId?.let { return it }
        val sessionId = UUID.randomUUID().toString()
        _uiState.update { it.copy(sessionId = sessionId) }
        return sessionId
    }

    private suspend fun collectResponse(text: String, sentAt: TimeMark) {
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
                    assistantResponses++
                    val assistantMessage = ChatMessage(text = response, sender = ChatMessage.Sender.ASSISTANT)
                    val turn = AiTurnInfo(aiSession.providerUsed ?: PROVIDER_NONE, aiSession.toolCallsThisTurn)
                    val feedbackEnabled = feedbackPromptRequester.isFeedbackEnabled()
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false,
                            turns = it.turns + (assistantMessage.id to turn),
                            feedbackEnabled = feedbackEnabled,
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

    /** Falha de gravação não pode derrubar o turno: a resposta continua na tela. */
    private suspend fun persistHistory() {
        val state = _uiState.value
        val sessionId = state.sessionId ?: return
        try {
            chatHistoryRepository.saveSession(
                sessionId = sessionId,
                messages = state.messages,
                shoppingId = state.shoppingId,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Failed to persist chat history")
        }
    }
}
