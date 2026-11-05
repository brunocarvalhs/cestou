package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class SpeechToTextManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(value = false)
    val isListening = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow<String?>(null)
    val recognizedText = _recognizedText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        mainHandler.post {
            if (!isRecognitionAvailable()) {
                Timber.w("SpeechRecognizer not available on this device")
                _errorMessage.value = "Voice recognition is not supported on this device"
                _isListening.value = false
                return@post
            }

            stopListening()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            runCatching {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Timber.d("SpeechRecognizer onReadyForSpeech")
                            _isListening.value = true
                            _errorMessage.value = null
                        }

                        override fun onBeginningOfSpeech() {
                            Timber.d("SpeechRecognizer onBeginningOfSpeech")
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // No-op
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {
                            // No-op
                        }

                        override fun onEndOfSpeech() {
                            Timber.d("SpeechRecognizer onEndOfSpeech")
                            _isListening.value = false
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client side error ($error)"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                                else -> "Speech recognition error ($error)"
                            }
                            _errorMessage.value = errorMsg
                            Timber.w("SpeechRecognizer error code: $error ($errorMsg)")
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            Timber.d("SpeechRecognizer onResults: %s", matches)
                            val resultText = matches?.firstOrNull()
                            if (!resultText.isNullOrBlank()) {
                                _recognizedText.value = resultText
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            Timber.d("SpeechRecognizer onPartialResults: %s", matches)
                            val partialText = matches?.firstOrNull()
                            if (!partialText.isNullOrBlank()) {
                                _recognizedText.value = partialText
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {
                            // No-op
                        }
                    })
                    startListening(intent)
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to start SpeechRecognizer")
                _isListening.value = false
                _errorMessage.value = "Failed to start speech recognition"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            _isListening.value = false
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
        }
    }

    fun clearResult() {
        _recognizedText.value = null
        _errorMessage.value = null
    }
}
