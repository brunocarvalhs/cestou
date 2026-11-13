package br.com.brunocarvalhs.howmuch.deeplink

import android.content.Intent
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.WearPairing

/** Interpreta um Intent de deep link e diz o que o app deve fazer com ele. */
internal interface DeepLinkParser {

    /** Devolve a ação do link, ou [DeepLinkAction.Ignored] quando o Intent não é um link do Cestou. */
    fun parse(intent: Intent): DeepLinkAction
}

/** O que um deep link pede. A Activity/ViewModel aplica a ação; o parser só interpreta. */
internal sealed interface DeepLinkAction {

    /** Pareamento pedido pelo relógio (sdd-055). */
    data class PairWatch(val request: WearPairing) : DeepLinkAction

    /** Criar lista pedida pelo relógio (sdd-056). */
    data object ShoppingCreate : DeepLinkAction

    /** Abrir o paywall pedido pelo relógio (cestou://subscription). */
    data object Subscription : DeepLinkAction

    /** Abrir uma conversa da IA. */
    data class AiChat(val sessionId: String) : DeepLinkAction

    /** Convite para entrar numa lista. */
    data class Invite(val token: String) : DeepLinkAction

    data object Ignored : DeepLinkAction
}
