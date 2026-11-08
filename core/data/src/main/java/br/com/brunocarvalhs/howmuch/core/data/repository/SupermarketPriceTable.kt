package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.ChainPriceIndex
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketChain

/**
 * Índices médios por região, em relação ao preço de varejo informado na lista. São estimativas
 * (atacarejo costuma ficar abaixo do varejo; a rede de bairro, perto ou acima dele), não
 * cotações em tempo real; a tela avisa isso ao usuário.
 */
internal object SupermarketPriceTable {

    private val ASSAI = SupermarketChain("assai", "Assaí")
    private val ATACADAO = SupermarketChain("atacadao", "Atacadão")
    private val DIA = SupermarketChain("dia", "Dia")
    private val GUANABARA = SupermarketChain("guanabara", "Guanabara")

    private enum class Region(val states: Set<String>) {
        SOUTHEAST(setOf("SP", "RJ", "MG", "ES")),
        SOUTH(setOf("PR", "SC", "RS")),
        NORTHEAST(setOf("BA", "SE", "AL", "PE", "PB", "RN", "CE", "PI", "MA")),
        NORTH(setOf("AM", "PA", "AC", "RO", "RR", "AP", "TO")),
        CENTER_WEST(setOf("DF", "GO", "MT", "MS"))
    }

    private val byRegion: Map<Region, List<ChainPriceIndex>> = mapOf(
        Region.SOUTHEAST to listOf(
            ChainPriceIndex(ATACADAO, 0.87),
            ChainPriceIndex(ASSAI, 0.89),
            ChainPriceIndex(DIA, 1.03)
        ),
        Region.SOUTH to listOf(
            ChainPriceIndex(ATACADAO, 0.88),
            ChainPriceIndex(ASSAI, 0.90),
            ChainPriceIndex(DIA, 1.02)
        ),
        Region.NORTHEAST to listOf(
            ChainPriceIndex(ASSAI, 0.86),
            ChainPriceIndex(ATACADAO, 0.88),
            ChainPriceIndex(DIA, 1.04)
        ),
        Region.NORTH to listOf(
            ChainPriceIndex(ATACADAO, 0.90),
            ChainPriceIndex(ASSAI, 0.91),
            ChainPriceIndex(DIA, 1.06)
        ),
        Region.CENTER_WEST to listOf(
            ChainPriceIndex(ATACADAO, 0.88),
            ChainPriceIndex(ASSAI, 0.90),
            ChainPriceIndex(DIA, 1.03)
        )
    )

    private val guanabaraRj = ChainPriceIndex(GUANABARA, 0.96)

    /** Redes da UF [state] (sigla); vazio quando a UF não é reconhecida. */
    fun forState(state: String): List<ChainPriceIndex> {
        val uf = state.trim().uppercase()
        val region = Region.entries.firstOrNull { uf in it.states } ?: return emptyList()
        val chains = byRegion.getValue(region)
        return if (uf == "RJ") chains + guanabaraRj else chains
    }
}
