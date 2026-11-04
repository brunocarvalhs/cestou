package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import android.content.Context
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Entrada por voz do chat: liga o [SpeechToTextManager] ao estado da tela e à telemetria. Fica fora
 * do `AiChatViewModel` para ele não passar do limite de funções do Detekt.
 */
internal class AiChatVoiceInput(
    private val speechToTextManager: SpeechToTextManager,
    private val telemetry: AiChatAnalytics,
    private val context: Context,
    private val state: MutableStateFlow<AiChatUiState>
) {

    /** Verdadeiro quando o texto a enviar veio do reconhecimento de voz, para o `input_method`. */
    private var inputFromVoice = false

    fun observe(scope: CoroutineScope) {
        scope.launch {
            speechToTextManager.isListening.collect { listening ->
                state.update { it.copy(isRecording = listening) }
            }
        }
        scope.launch {
            speechToTextManager.recognizedText.collect { text ->
                if (!text.isNullOrBlank()) {
                    onResult(text)
                }
            }
        }
        scope.launch {
            speechToTextManager.errorMessage.collect { error ->
                if (!error.isNullOrBlank()) {
                    telemetry.voiceFailed(VoiceFailure.RECOGNITION)
                    state.update { it.copy(voiceErrorMessage = error, isRecording = false) }
                }
            }
        }
    }

    /** O `voice_started` sai do ViewModel, junto do callback de intent que o dispara. */
    fun start() {
        if (speechToTextManager.isRecognitionAvailable()) {
            state.update { it.copy(isRecording = true, voiceErrorMessage = null) }
            speechToTextManager.clearResult()
            speechToTextManager.startListening()
        } else {
            telemetry.voiceFailed(VoiceFailure.UNSUPPORTED)
            state.update {
                it.copy(
                    voiceErrorMessage = context.getString(R.string.ai_chat_voice_not_supported),
                    isRecording = false
                )
            }
        }
    }

    fun stop() {
        speechToTextManager.stopListening()
        state.update { it.copy(isRecording = false) }
    }

    fun onResult(text: String) {
        inputFromVoice = true
        state.update { it.copy(input = text) }
    }

    /** Lê e zera a origem do texto: cada envio reporta o `input_method` uma única vez. */
    fun consumeInputFromVoice(): Boolean {
        val fromVoice = inputFromVoice
        inputFromVoice = false
        return fromVoice
    }
}
