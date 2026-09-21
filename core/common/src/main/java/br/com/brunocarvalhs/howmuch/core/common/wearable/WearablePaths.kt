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
    const val AUTH_PAIR = "/auth/pair"

    /** Must match the <item> value in wear/src/main/res/values/wear.xml. */
    const val WEAR_CAPABILITY = "howmuch_wear_app"

    /**
     * The phone app's applicationId. Wear intentionally has its own, distinct
     * applicationId (for separate Firebase App Distribution / Play Console
     * identity) - this constant is needed wherever wear-side code used to
     * assume `context.packageName` also identified the phone app.
     */
    const val PHONE_APPLICATION_ID = "br.com.brunocarvalhs.howmuch"
}
