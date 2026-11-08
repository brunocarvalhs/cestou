package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Praça escolhida pelo usuário; os índices de preço hoje variam por UF e a cidade só a identifica.
 * [state] é a sigla da UF (ex.: `RJ`) e [city] o nome livre da cidade.
 */
@Serializable
data class UserLocation(
    val state: String,
    val city: String
) {
    /** Chave estável da praça, usada no cache local de índices. */
    val key: String get() = "${state.trim().uppercase()}|${city.trim().lowercase()}"

    companion object {
        /** Siglas das 27 UFs aceitas como [state]. */
        val BRAZILIAN_STATES = listOf(
            "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO", "MA", "MG", "MS", "MT", "PA",
            "PB", "PE", "PI", "PR", "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
        )
    }
}

/** Rede de supermercado comparada; [id] é estável e [name] é o nome de exibição (marca). */
@Serializable
data class SupermarketChain(
    val id: String,
    val name: String
)

/**
 * Índice médio de preço de uma rede numa praça, relativo ao preço de varejo registrado pelo
 * usuário na lista (`1.0` = igual; `0.9` = 10% mais barato).
 */
data class ChainPriceIndex(
    val chain: SupermarketChain,
    val index: Double
)

/** Estimativa de uma rede: [estimatedTotal] da lista e [savings] frente à rede mais cara do ranking. */
data class ChainQuote(
    val chain: SupermarketChain,
    val estimatedTotal: Double,
    val savings: Double
)

/**
 * Resultado da comparação. [quotes] vem ordenada da mais barata para a mais cara;
 * [baseTotal] é a soma dos itens com preço e [unpricedCount] quantos itens ficaram de fora dela.
 */
data class SupermarketPriceComparison(
    val location: UserLocation,
    val baseTotal: Double,
    val unpricedCount: Int,
    val quotes: List<ChainQuote>
) {
    val cheapest: ChainQuote? get() = quotes.firstOrNull()

    /** Economia de escolher a rede mais barata em vez da mais cara. */
    val maxSavings: Double get() = quotes.maxOfOrNull { it.savings } ?: 0.0
}
