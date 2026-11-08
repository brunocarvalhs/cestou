package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

private const val MAX_ITEMS = 20
private const val MAX_ITEM_LENGTH = 60
private const val MAX_ITEM_WORDS = 4

private val listLine = Regex("""^\s*(?:[-*•]|\d{1,2}[.)])\s+(.+)$""")
private val emphasis = Regex("""[*_`]+""")
private val descriptionSeparator = Regex("""\s+[-–—:]\s+|:\s+""")
private val sentenceEnd = Regex("""[.!?]$""")

/** Item de compra é curto e não é frase: passos de receita e dicas têm mais palavras ou terminam em ponto. */
private fun String.looksLikeItem(): Boolean =
    isNotEmpty() && length <= MAX_ITEM_LENGTH &&
        split(Regex("""\s+""")).size <= MAX_ITEM_WORDS && !sentenceEnd.containsMatchIn(this)

/**
 * Extrai os itens sugeridos de uma resposta da IA em markdown: uma linha por item, em lista com
 * marcador ou numerada. Sem formato estruturado do provedor, a lista é o único contrato estável.
 * Só vale se a maioria das linhas da lista parece item; do contrário é passo a passo ou dica, e
 * nada é oferecido para adicionar.
 */
internal fun parseSuggestedItems(markdown: String): List<String> {
    val candidates = markdown.lineSequence()
        .mapNotNull { line -> listLine.find(line)?.groupValues?.get(1) }
        .map { raw -> raw.replace(emphasis, "").split(descriptionSeparator, limit = 2).first().trim() }
        .toList()
    val items = candidates.filter { it.looksLikeItem() }
    if (items.size * 2 <= candidates.size) return emptyList()
    return items.distinctBy { it.lowercase() }.take(MAX_ITEMS)
}
