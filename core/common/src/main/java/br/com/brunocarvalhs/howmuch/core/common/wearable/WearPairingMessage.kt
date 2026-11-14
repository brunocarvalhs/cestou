package br.com.brunocarvalhs.howmuch.core.common.wearable

/**
 * A pairing message received from the other device, already decoded.
 * Both apps use it: the watch receives [PhoneKey] and [Error]; the phone
 * receives [WatchConfirmed].
 */
sealed interface WearPairingMessage {
    val sessionId: String

    /** Watch side: the phone's public key for this session. [sourceNodeId] is the phone node to reply to. */
    data class PhoneKey(
        override val sessionId: String,
        val phonePublicKey: String,
        val sourceNodeId: String
    ) : WearPairingMessage

    /** Watch side: the pairing failed on the phone, or the watch failed locally and reports it here. */
    data class Error(override val sessionId: String, val reason: String) : WearPairingMessage

    /** Phone side: the watch user tapped "Parear" for this session. */
    data class WatchConfirmed(override val sessionId: String, val sourceNodeId: String) : WearPairingMessage
}
