package br.com.brunocarvalhs.howmuch.core.common.util

import android.net.Uri

/**
 * Builds and parses shopping-list invite links (`https://<HOST>/join/<token>`).
 *
 * A QR code that only carries a bare token (e.g. "ABC123") is read by the phone's default
 * camera app as plain text, not as something that opens Cestou - there is nothing in a bare
 * string for Android to match against an app's intent filters. Encoding an actual link instead
 * lets the OS route it straight to this app via App Links (see the `autoVerify` intent-filter in
 * AndroidManifest.xml) - and to the Play Store when the app is not installed yet.
 *
 * [HOST] must stay in sync with that manifest intent-filter and with the
 * `https://<HOST>/.well-known/assetlinks.json` file that verifies the App Link. It is a
 * placeholder until a production domain (or Firebase Hosting project) is wired up.
 */
object InviteLink {

    // TODO: replace with the real host once the domain / Firebase Hosting project is set up,
    // and update the matching <data android:host="..."> in AndroidManifest.xml.
    const val HOST = "cestou.app"

    private const val PATH_SEGMENT = "join"
    private const val QUERY_PARAM_TOKEN = "token"

    fun build(token: String): String = "https://$HOST/$PATH_SEGMENT/$token"

    /** True when [uri] is one of our invite links, regardless of whether it carries a token. */
    fun matches(uri: Uri): Boolean = uri.host == HOST && uri.pathSegments.firstOrNull() == PATH_SEGMENT

    /** The token carried by [uri] - from the `token` query param, or the path segment after `/join`. */
    fun tokenFrom(uri: Uri): String? =
        uri.getQueryParameter(QUERY_PARAM_TOKEN)?.takeIf { it.isNotBlank() }
            ?: uri.pathSegments.getOrNull(1)?.takeIf { it.isNotBlank() }

    /**
     * Extracts an invite token from arbitrary scanned/pasted text: a full invite link, or a bare
     * token. QR codes generated before the link format existed, and manual token entry, still
     * only carry the raw token - so anything that isn't a link is returned as-is.
     */
    fun extractToken(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val uri = Uri.parse(trimmed)
        // A URI with an unrelated host must not be scraped for something that looks like a token
        // (e.g. an arbitrary link's second path segment) - only our own invite links qualify.
        return if (uri.scheme == null) trimmed else tokenFrom(uri).takeIf { matches(uri) }
    }
}
