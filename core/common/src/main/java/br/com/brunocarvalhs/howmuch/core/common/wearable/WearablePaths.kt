package br.com.brunocarvalhs.howmuch.core.common.wearable

/**
 * Shared between WearableSyncServiceImpl (called from the phone app) and
 * WearAuthListenerService (running in the wear app, a different Gradle
 * module) - both must agree on these exact strings for the pairing
 * handshake and capability lookup to match.
 */
object WearablePaths {
    const val PAIRING_VERIFY = "/pairing/verify"
    const val PAIRING_CONFIRM = "/pairing/confirm"
    /** Payload: [encodeAuthPair]. The watch signs in to Firebase with the token (AD-009). */
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

    fun encodeAuthPair(userId: String, googleIdToken: String): ByteArray =
        "$userId\n$googleIdToken".toByteArray()

    /** Returns (userId, googleIdToken), or null for a malformed or legacy (uid-only) payload. */
    fun decodeAuthPair(data: ByteArray): Pair<String, String>? {
        val parts = String(data).split('\n')
        return if (parts.size == 2 && parts.all { it.isNotBlank() }) parts[0] to parts[1] else null
    }
}
