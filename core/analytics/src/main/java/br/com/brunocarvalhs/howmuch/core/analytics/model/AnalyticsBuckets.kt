package br.com.brunocarvalhs.howmuch.core.analytics.model

/**
 * Faixas para parâmetros numéricos. Mandar a faixa em vez do valor mantém a cardinalidade baixa
 * no GA4 e evita expor tamanhos ou valores exatos do conteúdo do usuário.
 */
object AnalyticsBuckets {

    private const val SHORT_TEXT_MAX = 20
    private const val MEDIUM_TEXT_MAX = 80
    private const val FEW_ITEMS_MAX = 5
    private const val SOME_ITEMS_MAX = 20
    private const val GOAL_LOW_MAX = 500.0
    private const val GOAL_MEDIUM_MAX = 1500.0
    private const val GOAL_HIGH_MAX = 3000.0

    /** `0`, `1-20`, `21-80` ou `81+` caracteres. */
    fun length(chars: Int): String = when {
        chars <= 0 -> "0"
        chars <= SHORT_TEXT_MAX -> "1-20"
        chars <= MEDIUM_TEXT_MAX -> "21-80"
        else -> "81+"
    }

    /** Faixa de uma meta mensal, sem expor o valor: `<500`, `500-1500`, `1500-3000` ou `3000+`. */
    fun goal(amount: Double): String = when {
        amount < GOAL_LOW_MAX -> "<500"
        amount < GOAL_MEDIUM_MAX -> "500-1500"
        amount < GOAL_HIGH_MAX -> "1500-3000"
        else -> "3000+"
    }

    /** `0`, `1-5`, `6-20` ou `21+` itens. */
    fun count(items: Int): String = when {
        items <= 0 -> "0"
        items <= FEW_ITEMS_MAX -> "1-5"
        items <= SOME_ITEMS_MAX -> "6-20"
        else -> "21+"
    }
}
