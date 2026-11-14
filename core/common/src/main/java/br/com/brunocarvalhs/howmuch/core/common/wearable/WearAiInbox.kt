package br.com.brunocarvalhs.howmuch.core.common.wearable

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge between the watch's message listener and the AI quick-entry screen. Same idea
 * as [WearPairingInbox]: the listener runs outside any screen, so answers are pushed here and
 * collected by the ViewModel that asked. An answer nobody collects is dropped, and the ViewModel's
 * timeout covers that case.
 *
 * Public: the watch module's listener (WearAuthListenerService) emits into it and feature:shopping
 * collects it, so it crosses modules.
 */
@Singleton
class WearAiInbox @Inject constructor() {
    private val _messages = MutableSharedFlow<WearAiMessage>(extraBufferCapacity = BUFFER_CAPACITY)
    val messages: SharedFlow<WearAiMessage> = _messages

    fun emit(message: WearAiMessage) {
        _messages.tryEmit(message)
    }

    private companion object {
        const val BUFFER_CAPACITY = 16
    }
}
