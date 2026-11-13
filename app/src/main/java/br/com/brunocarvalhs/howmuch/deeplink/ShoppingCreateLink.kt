package br.com.brunocarvalhs.howmuch.deeplink

import android.net.Uri

/**
 * Reads the watch's "create list" deeplink (sdd-056): `cestou://shopping/new`. Any other URI,
 * including `cestou://home`, is not this request, so other deeplinks are unaffected.
 */
internal object ShoppingCreateLink {

    private const val SCHEME = "cestou"
    private const val HOST = "shopping"
    private const val PATH = "/new"

    fun matches(uri: Uri): Boolean = uri.scheme == SCHEME && uri.host == HOST && uri.path == PATH
}
