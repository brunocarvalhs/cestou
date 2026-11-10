package br.com.brunocarvalhs.howmuch.feature.shopping.navigation.wear

import kotlinx.serialization.Serializable

/** Relógio: lista completa com busca (sdd-056). Interna ao módulo, sem contrato público. */
@Serializable
internal data object WearSearch

/** Relógio: análise mensal das compras finalizadas (sdd-056). Interna ao módulo. */
@Serializable
internal data object WearHistory

/** Relógio: menu com as funcionalidades secundárias (Histórico, Configurações). Interna ao módulo. */
@Serializable
internal data object WearMenu
