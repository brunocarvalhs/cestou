package br.com.brunocarvalhs.howmuch.core.ui.contract

import android.app.Activity

/**
 * Intersticial de tela inteira mostrado depois da primeira resposta da IA do usuário FREE.
 *
 * Interface em `core:ui` para que `feature:chat` não dependa do SDK de anúncios. A implementação
 * fica em `core:ads` e é fornecida pelo `:app` via `LocalAiInterstitial`. Quem chama decide a
 * elegibilidade (plano FREE e flag); aqui não há consulta de assinatura.
 */
interface AiInterstitialHook {

    /** Pede consentimento quando necessário e carrega o anúncio. */
    suspend fun prepare(activity: Activity)

    /** Mostra o anúncio se estiver carregado. Retorna `false` sem bloquear se não estiver. */
    fun showIfReady(activity: Activity): Boolean
}
