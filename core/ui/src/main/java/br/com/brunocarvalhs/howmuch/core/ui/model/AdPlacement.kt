package br.com.brunocarvalhs.howmuch.core.ui.model

/**
 * Onde um banner de anúncio pode aparecer para o plano FREE (sdd-059). Só as telas aprovadas
 * estão aqui; quem chama [br.com.brunocarvalhs.howmuch.core.ui.contract.LocalAdBanner] escolhe
 * uma delas.
 */
enum class AdPlacement {
    CART,
    HISTORY
}
