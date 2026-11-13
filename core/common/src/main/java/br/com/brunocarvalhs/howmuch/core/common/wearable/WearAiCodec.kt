package br.com.brunocarvalhs.howmuch.core.common.wearable

/**
 * Payloads of the watch's AI relay. Paths are in [WearablePaths]; this is the layout.
 * Fields are UTF-8 joined by '\n'. Free text (prompt, answer, summary, reason) is always the last
 * field, so it may contain the separator itself.
 *
 * Public because the watch module and the phone app are separate Gradle modules and both encode or
 * decode these payloads, so they cannot be internal to core:common.
 */
object WearAiCodec {

    /** Watch -> phone. Returns (requestId, prompt), or null when either is blank. */
    fun encodePrompt(requestId: String, prompt: String): ByteArray = joinWithTail(requestId, tail = prompt)

    fun decodePrompt(data: ByteArray): Pair<String, String>? = splitRequiredText(data, headCount = 1)

    /** Phone -> watch. The answer may be empty, so only the requestId is required. */
    fun encodeReply(requestId: String, text: String): ByteArray = joinWithTail(requestId, tail = text)

    fun decodeReply(data: ByteArray): Pair<String, String>? =
        splitWithTail(data, headCount = 1)?.let { (head, tail) -> head[0] to tail }

    /** Phone -> watch. Returns (requestId, actionId, summary); the summary may be empty. */
    fun encodeConfirmRequest(requestId: String, actionId: String, summary: String): ByteArray =
        joinWithTail(requestId, actionId, tail = summary)

    fun decodeConfirmRequest(data: ByteArray): Triple<String, String, String>? =
        splitWithTail(data, headCount = 2)?.let { (head, tail) -> Triple(head[0], head[1], tail) }

    /** Watch -> phone. Returns (requestId, actionId, approved), or null for an unknown decision. */
    fun encodeConfirm(requestId: String, actionId: String, approved: Boolean): ByteArray =
        joinWithTail(requestId, actionId, tail = if (approved) APPROVED else REJECTED)

    fun decodeConfirm(data: ByteArray): Triple<String, String, Boolean>? =
        splitWithTail(data, headCount = 2)?.let { (head, decision) ->
            when (decision) {
                APPROVED -> Triple(head[0], head[1], true)
                REJECTED -> Triple(head[0], head[1], false)
                else -> null
            }
        }

    /** Phone -> watch. Returns (requestId, reason), or null when either is blank. */
    fun encodeFailure(requestId: String, reason: String): ByteArray = joinWithTail(requestId, tail = reason)

    fun decodeFailure(data: ByteArray): Pair<String, String>? = splitRequiredText(data, headCount = 1)

    private const val APPROVED = "1"
    private const val REJECTED = "0"
}
