package br.com.brunocarvalhs.howmuch.core.common.wearable

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge between the message listeners (WearAuthListenerService on the watch,
 * the phone's WearPairingListenerService) and the pairing screens. Listeners run
 * outside any screen, so messages are pushed here and collected by whichever
 * ViewModel owns the active session. Messages nobody collects are dropped: the
 * session timeouts make the sender give up in that case.
 */
@Singleton
class WearPairingInbox @Inject constructor() {
    private val _messages = MutableSharedFlow<WearPairingMessage>(extraBufferCapacity = BUFFER_CAPACITY)
    val messages: SharedFlow<WearPairingMessage> = _messages

    fun emit(message: WearPairingMessage) {
        _messages.tryEmit(message)
    }

    private companion object {
        const val BUFFER_CAPACITY = 16
    }
}
