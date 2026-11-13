package br.com.brunocarvalhs.howmuch.core.common.wearable

import kotlinx.coroutines.flow.Flow

/**
 * The AI quick entry's link between the watch and the phone. Kept apart from
 * [WearableSyncService] so each contract stays small: this one is only about the AI relay.
 *
 * Public: implemented in core:data and used by feature:shopping (watch) and app (phone), so it crosses
 * modules.
 */
interface WearableAiLink {
    /**
     * Watch: sends [prompt] to the nearest phone, which runs the agent with its own key.
     * Returns false when no phone is reachable; the answer arrives through [observeAiMessages].
     */
    suspend fun sendAiPrompt(requestId: String, prompt: String): Boolean

    /** Watch: answers an approval request of [requestId]. Returns false when no phone is reachable. */
    suspend fun sendAiConfirm(requestId: String, actionId: String, approved: Boolean): Boolean

    /** Phone: sends an AI relay message to the watch node that asked. Returns false when the send failed. */
    suspend fun sendAiMessage(watchNodeId: String, message: WearAiMessage): Boolean

    /**
     * Watch: emits the phone's messages for [requestId]. Collect it only while waiting; it stops
     * on the caller's cancellation, and a message nobody collects is dropped.
     */
    fun observeAiMessages(requestId: String): Flow<WearAiMessage>
}
