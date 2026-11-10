package br.com.brunocarvalhs.howmuch.core.common.wearable

/**
 * Shared between WearableSyncServiceImpl (called from the phone app) and
 * WearAuthListenerService (running in the wear app, a different Gradle
 * module) - both must agree on these exact strings and payload layouts
 * for the pairing handshake to match.
 *
 * Payloads are UTF-8 fields joined by '\n'. Session ids are base64url, so
 * they never contain the separator.
 */
object WearablePaths {
    /** Phone -> watch: sessionId, phone public key (base64url). */
    const val PAIRING_PHONE_KEY = "/pairing/phone-key"

    /** Watch -> phone: sessionId. Sent when the watch user taps "Parear". */
    const val PAIRING_WEAR_CONFIRM = "/pairing/wear-confirm"

    /** Either side -> the other: sessionId, reason (a PairingFailure name). */
    const val PAIRING_ERROR = "/pairing/error"

    /** Phone -> watch, only after both sides confirmed: sessionId, userId, Google ID token. */
    const val AUTH_PAIR = "/auth/pair"

    /** Sent by the phone when it signs out, so the watch drops its own Firebase session too. */
    const val AUTH_SIGN_OUT = "/auth/sign-out"

    /** Must match the <item> value in wear/src/main/res/values/wear.xml. */
    const val WEAR_CAPABILITY = "howmuch_wear_app"

    /**
     * The phone app's applicationId. Wear intentionally has its own, distinct
     * applicationId (for separate Firebase App Distribution / Play Console
     * identity) - this constant is needed wherever wear-side code used to
     * assume `context.packageName` also identified the phone app.
     */
    const val PHONE_APPLICATION_ID = "br.com.brunocarvalhs.howmuch"

    fun encodePhoneKey(sessionId: String, phonePublicKey: String): ByteArray = join(sessionId, phonePublicKey)

    /** Returns (sessionId, phonePublicKey), or null for a malformed payload. */
    fun decodePhoneKey(data: ByteArray): Pair<String, String>? = split(data, 2)?.let { it[0] to it[1] }

    fun encodeWearConfirm(sessionId: String): ByteArray = sessionId.toByteArray()

    /** Returns the sessionId, or null when the payload is empty or malformed. */
    fun decodeWearConfirm(data: ByteArray): String? = split(data, 1)?.first()

    fun encodeError(sessionId: String, reason: String): ByteArray = join(sessionId, reason)

    /** Returns (sessionId, reason), or null for a malformed payload. */
    fun decodeError(data: ByteArray): Pair<String, String>? = split(data, 2)?.let { it[0] to it[1] }

    fun encodeAuthPair(sessionId: String, userId: String, googleIdToken: String): ByteArray =
        join(sessionId, userId, googleIdToken)

    /** Returns (sessionId, userId, googleIdToken), or null for a malformed payload. */
    fun decodeAuthPair(data: ByteArray): Triple<String, String, String>? =
        split(data, AUTH_PAIR_FIELDS)?.let { Triple(it[0], it[1], it[2]) }

    private fun join(vararg fields: String): ByteArray = fields.joinToString(SEPARATOR).toByteArray()

    /** Returns exactly [expected] non-blank fields, or null. */
    private fun split(data: ByteArray, expected: Int): List<String>? {
        val parts = String(data).split(SEPARATOR)
        return if (parts.size == expected && parts.all { it.isNotBlank() }) parts else null
    }

    private const val SEPARATOR = "\n"
    private const val AUTH_PAIR_FIELDS = 3
}
