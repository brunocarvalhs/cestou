package br.com.brunocarvalhs.howmuch.wear.presentation.session

import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearSubscriptionStore
import javax.inject.Inject

/**
 * Apaga no relógio o que só faz sentido com uma sessão ativa: a última lista (tile) e o status
 * PRO. Chamado quando a sessão termina, seja por "Sair" no relógio ou por sair no celular.
 */
class WearSessionCleaner @Inject constructor(
    private val wearListStore: WearListStore,
    private val wearSubscriptionStore: WearSubscriptionStore
) {
    suspend fun clear() {
        wearListStore.clear()
        wearSubscriptionStore.clear()
    }
}
