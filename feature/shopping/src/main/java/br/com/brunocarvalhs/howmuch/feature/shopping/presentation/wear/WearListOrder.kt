package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping

/** Quantas listas a home do relógio mostra antes do botão "Mais" (sdd-056). */
internal const val WEAR_HOME_RECENT_LIMIT = 5

/**
 * Listas ativas na ordem do relógio: fixadas primeiro, depois as criadas por último (sdd-056, Q15).
 * Não usa a data de abertura. `sortedWith` é estável, então empates mantêm a ordem de origem.
 */
internal fun List<Shopping>.wearActiveLists(): List<Shopping> = asSequence()
    .filter { it.status != Shopping.Status.FINISH }
    .sortedWith(compareByDescending<Shopping> { it.isFavorite }.thenByDescending { it.createdAt })
    .toList()

/** Listas finalizadas, da compra mais recente para a mais antiga. */
internal fun List<Shopping>.wearArchivedLists(): List<Shopping> = asSequence()
    .filter { it.status == Shopping.Status.FINISH }
    .sortedByDescending { it.finishedAt ?: it.createdAt }
    .toList()
