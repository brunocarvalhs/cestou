package br.com.brunocarvalhs.howmuch.core.data.wearable

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiCodec
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableAiLink
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import javax.inject.Inject

/** Data Layer side of the watch's AI quick entry. Payloads are in [WearAiCodec]. */
internal class WearableAiLinkImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wearAiInbox: WearAiInbox
) : WearableAiLink {

    override suspend fun sendAiPrompt(requestId: String, prompt: String): Boolean {
        val nodeId = nearestPhoneNodeId() ?: return false
        return sendWearableMessage(
            context,
            nodeId,
            WearablePaths.AI_PROMPT,
            WearAiCodec.encodePrompt(requestId, prompt)
        )
    }

    override suspend fun sendAiConfirm(requestId: String, actionId: String, approved: Boolean): Boolean {
        val nodeId = nearestPhoneNodeId() ?: return false
        return sendWearableMessage(
            context,
            nodeId,
            WearablePaths.AI_CONFIRM,
            WearAiCodec.encodeConfirm(requestId, actionId, approved)
        )
    }

    override suspend fun sendAiMessage(watchNodeId: String, message: WearAiMessage): Boolean {
        val path = when (message) {
            is WearAiMessage.Reply -> WearablePaths.AI_REPLY
            is WearAiMessage.ConfirmRequest -> WearablePaths.AI_CONFIRM_REQUEST
            is WearAiMessage.Failure -> WearablePaths.AI_FAILURE
        }
        val payload = when (message) {
            is WearAiMessage.Reply -> WearAiCodec.encodeReply(message.requestId, message.text)
            is WearAiMessage.ConfirmRequest ->
                WearAiCodec.encodeConfirmRequest(message.requestId, message.actionId, message.summary)
            is WearAiMessage.Failure -> WearAiCodec.encodeFailure(message.requestId, message.reason)
        }
        return sendWearableMessage(context, watchNodeId, path, payload)
    }

    override fun observeAiMessages(requestId: String): Flow<WearAiMessage> =
        wearAiInbox.messages.filter { it.requestId == requestId }

    /** Null when the phone cannot be found; a lookup error counts as unreachable, not as a crash. */
    private suspend fun nearestPhoneNodeId(): String? = runCatching { connectedPhoneNodeId(context) }
        .onFailure { error ->
            if (error is CancellationException) throw error
            Timber.tag("WearableSync").e(error, "Could not look up the phone node")
        }
        .getOrNull()
}
