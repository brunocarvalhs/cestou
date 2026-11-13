package br.com.brunocarvalhs.howmuch.deeplink

import android.net.Uri
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.WearPairing

/**
 * Reads the watch's pairing deeplink (sdd-055): `cestou://wear/pair?sid=...&k=...&n=...`.
 * Returns null for any other URI, or when a parameter is missing, so other deeplinks are unaffected.
 */
internal object WearPairingLink {

    private const val SCHEME = "cestou"
    private const val HOST = "wear"
    private const val PATH = "/pair"
    private const val PARAM_SESSION = "sid"
    private const val PARAM_WATCH_KEY = "k"
    private const val PARAM_WATCH_NODE = "n"

    fun parse(uri: Uri): WearPairing? {
        if (uri.scheme != SCHEME || uri.host != HOST || uri.path != PATH) return null
        val sessionId = uri.getQueryParameter(PARAM_SESSION)
        val watchKey = uri.getQueryParameter(PARAM_WATCH_KEY)
        val watchNode = uri.getQueryParameter(PARAM_WATCH_NODE)
        return if (sessionId.isNullOrBlank() || watchKey.isNullOrBlank() || watchNode.isNullOrBlank()) {
            null
        } else {
            WearPairing(sessionId = sessionId, wearPublicKey = watchKey, wearNodeId = watchNode)
        }
    }
}
