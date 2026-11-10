package br.com.brunocarvalhs.howmuch.core.common.wearable

/**
 * Messages of the watch's AI quick entry. The watch sends the prompt and the approval
 * answer; the phone sends back one of these three. Every message carries the request id, so
 * concurrent watch screens never mix answers.
 *
 * Public: the phone app (WearAiRelay) builds these and the watch module and feature:shopping read
 * them, so the type must be visible across modules.
 */
sealed interface WearAiMessage {
    val requestId: String

    /** The answer to the prompt. The text may be empty. */
    data class Reply(override val requestId: String, val text: String) : WearAiMessage

    /** The agent wants to run an irreversible action; the watch must ask the user (`approved` goes back). */
    data class ConfirmRequest(
        override val requestId: String,
        val actionId: String,
        val summary: String
    ) : WearAiMessage

    /** The prompt could not be answered. [reason] is an [AiFailure] name. */
    data class Failure(override val requestId: String, val reason: String) : WearAiMessage
}

/** Why the phone could not answer. Sent as a name, so the watch shows a message without guessing. */
object AiFailure {
    /** No AI provider is configured on the phone. */
    const val NO_PROVIDER = "NO_PROVIDER"

    /** Anything else went wrong on the phone. */
    const val ERROR = "ERROR"
}
