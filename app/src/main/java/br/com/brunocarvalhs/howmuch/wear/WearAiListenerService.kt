package br.com.brunocarvalhs.howmuch.wear

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiCodec
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives the watch's AI quick-entry messages: the prompt on `/ai/prompt` and the
 * approval answer on `/ai/confirm`. It routes them to [WearAiRelay] and never logs their content.
 *
 * Declared in the app manifest with the `/ai` prefix, so the system starts it when a message
 * arrives, even if the phone app is closed.
 */
@AndroidEntryPoint
class WearAiListenerService : WearableListenerService() {

    @Inject
    lateinit var relay: WearAiRelay

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearablePaths.AI_PROMPT -> onPrompt(messageEvent)
            WearablePaths.AI_CONFIRM -> onConfirm(messageEvent)
        }
    }

    private fun onPrompt(messageEvent: MessageEvent) {
        val decoded = WearAiCodec.decodePrompt(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            val (requestId, prompt) = decoded
            relay.answer(messageEvent.sourceNodeId, requestId, prompt)
        }
    }

    private fun onConfirm(messageEvent: MessageEvent) {
        val decoded = WearAiCodec.decodeConfirm(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            val (_, actionId, approved) = decoded
            relay.confirm(actionId, approved)
        }
    }

    private fun logMalformed(messageEvent: MessageEvent) {
        Timber.tag("WearAi").w("Ignoring a malformed message on %s", messageEvent.path)
    }
}
