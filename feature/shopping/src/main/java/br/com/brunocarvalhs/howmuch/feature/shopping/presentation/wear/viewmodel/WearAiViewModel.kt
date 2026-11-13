package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableAiLink
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearAiIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearAiUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

/**
 * Teto do prompt. Acima dele a mensagem estoura o limite da Data Layer (~100 KB no total), e o envio
 * falharia com uma mensagem enganosa de "celular não encontrado". A tela também limita a digitação.
 */
internal const val WEAR_AI_MAX_PROMPT_LENGTH = 300

/**
 * Entrada rápida de IA no relógio. Envia o prompt ao celular, que roda o agente com a
 * própria chave, e mostra a resposta. Ações de escrita só executam depois de [WearAiIntent.Decide]
 * com `approved = true`. Uma troca por vez: enquanto espera, o envio fica bloqueado.
 */
@HiltViewModel
class WearAiViewModel @Inject constructor(
    private val wearableAi: WearableAiLink,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<WearAiUiState>(WearAiUiState.Idle)
    val uiState: StateFlow<WearAiUiState> = _uiState.asStateFlow()

    private var exchange: Job? = null

    fun onIntent(intent: WearAiIntent) {
        when (intent) {
            is WearAiIntent.Send -> send(intent.prompt)
            is WearAiIntent.Decide -> decide(intent.approved)
            WearAiIntent.Reset -> reset()
        }
    }

    private fun send(prompt: String) {
        val text = prompt.trim()
        val current = _uiState.value
        if (text.isEmpty() || text.length > WEAR_AI_MAX_PROMPT_LENGTH) return
        if (current is WearAiUiState.Waiting || current is WearAiUiState.Confirming) return
        exchange?.cancel()
        exchange = viewModelScope.launch { runExchange(UUID.randomUUID().toString(), text) }
    }

    /**
     * One exchange. Its own [coroutineScope] keeps the listener tied to this request: cancelling the
     * exchange (a new prompt, or the phone is gone) also stops the listener, so an old answer can
     * never overwrite the current screen.
     */
    private suspend fun runExchange(requestId: String, prompt: String) = coroutineScope {
        _uiState.value = WearAiUiState.Waiting
        analytics.trackEvent(AnalyticsEvents.WEAR_AI_PROMPT_SENT)
        val answer = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(REPLY_TIMEOUT_MS) {
                wearableAi.observeAiMessages(requestId)
                    .onEach { message ->
                        if (message is WearAiMessage.ConfirmRequest) {
                            _uiState.value = WearAiUiState.Confirming(requestId, message.actionId, message.summary)
                        }
                    }
                    .first { it !is WearAiMessage.ConfirmRequest }
            }
        }
        if (!wearableAi.sendAiPrompt(requestId, prompt)) {
            answer.cancel()
            _uiState.value = WearAiUiState.Failed(WearAiUiState.PHONE_UNAVAILABLE)
            return@coroutineScope
        }
        _uiState.value = when (val message = answer.await()) {
            is WearAiMessage.Reply -> WearAiUiState.Answer(message.text)
            is WearAiMessage.Failure -> WearAiUiState.Failed(message.reason)
            else -> WearAiUiState.Failed(WearAiUiState.TIMEOUT)
        }
    }

    private fun reset() {
        val current = _uiState.value
        if (current is WearAiUiState.Answer || current is WearAiUiState.Failed) {
            _uiState.value = WearAiUiState.Idle
        }
    }

    private fun decide(approved: Boolean) {
        val pending = _uiState.value as? WearAiUiState.Confirming ?: return
        _uiState.value = WearAiUiState.Waiting
        analytics.trackEvent(AnalyticsEvents.WEAR_AI_ACTION_CONFIRMED, mapOf("approved" to approved))
        viewModelScope.launch {
            if (!wearableAi.sendAiConfirm(pending.requestId, pending.actionId, approved)) {
                exchange?.cancel()
                _uiState.value = WearAiUiState.Failed(WearAiUiState.PHONE_UNAVAILABLE)
            }
        }
    }

    private companion object {
        /**
         * Maior que o tempo de aprovação do celular (APPROVAL_TIMEOUT_MS, 120 s): a resposta chega
         * antes do corte.
         */
        const val REPLY_TIMEOUT_MS = 150_000L
    }
}
