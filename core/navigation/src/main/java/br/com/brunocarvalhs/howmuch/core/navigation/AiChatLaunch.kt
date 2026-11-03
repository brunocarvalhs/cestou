package br.com.brunocarvalhs.howmuch.core.navigation

import android.net.Uri

/**
 * Deeplink `cestou://chat/<sessionId>`: o toque numa notificação da IA abre essa conversa. Fica em
 * `core:navigation` para a notificação (`feature:chat`) e o `app` usarem o mesmo formato.
 */
object AiChatLaunch {
    const val SCHEME = "cestou"
    const val HOST = "chat"

    fun uriFor(sessionId: String): Uri = Uri.Builder().scheme(SCHEME).authority(HOST).appendPath(sessionId).build()

    /** Id da conversa de um link `cestou://chat/<id>`, ou `null` se [uri] não for um. */
    fun sessionIdFrom(uri: Uri): String? {
        if (uri.host != HOST || uri.scheme != SCHEME) return null
        return uri.pathSegments.singleOrNull()?.takeIf { it.isNotBlank() }
    }
}
