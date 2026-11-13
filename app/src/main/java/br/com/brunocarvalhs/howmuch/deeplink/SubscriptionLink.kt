package br.com.brunocarvalhs.howmuch.deeplink

import android.net.Uri

/**
 * Reads the watch's "open plan" deeplink: `cestou://subscription`. Any other URI, including
 * `cestou://home`, is not this request, so the home deeplink keeps only opening the app.
 */
internal object SubscriptionLink {

    private const val SCHEME = "cestou"
    private const val HOST = "subscription"

    fun matches(uri: Uri): Boolean = uri.scheme == SCHEME && uri.host == HOST
}
