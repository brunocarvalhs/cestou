package br.com.brunocarvalhs.howmuch.core.ai.prompt

/**
 * Prompt e limpeza da leitura de produtos por foto. O modelo é instruído a ignorar ruído, e
 * [sanitizeNames] garante isso de novo no código: o prompt reduz o texto espúrio, mas não o elimina.
 *
 * `public` porque `feature:products` consome e `core:ai` não pode depender dele.
 */
object VisionExtractionPrompt {

    val PROMPT: String = """
        Analise esta imagem e identifique os produtos de supermercado presentes nela.
        Extraia somente nomes comerciais de produtos e ingredientes úteis para uma lista de compras.
        IGNORE por completo: tabelas nutricionais (calorias, sódio, gorduras, carboidratos, proteínas, porções, %VD),
        códigos, lotes, datas de validade, códigos de barras e sequências aleatórias como "1a00".
        Para cada produto, extraia: nome, quantidade estimada, unidade de medida (un, kg, L, pct, etc) e a categoria mais provável.
        Responda apenas com um array JSON válido no formato: [{"name": "...", "quantity": 0.0, "unit": "...", "category": "..."}]
        Categorias válidas: Hortifruti, Carnes, Laticínios, Bebidas, Limpeza, Higiene, Mercearia, Legumes, Perecíveis, Congelados, Padaria, Outros.
        Se não houver nenhum produto identificável, responda com [].
        Não inclua nenhuma outra explicação ou formatação markdown além do JSON.
    """.trimIndent()

    private const val MIN_LENGTH = 2
    private const val MAX_LENGTH = 60

    /** Código curto, sem espaços, que mistura dígitos a letras ou é só número ("1a00", "7891000"). */
    private val codeLike = Regex("""^(?=.*\d)[\p{L}\d.\-/]{1,12}$""")

    private val nutritionTerms = listOf(
        "kcal", "valor energético", "valor energetico", "sódio", "sodio", "gorduras", "carboidrato",
        "proteína", "proteina", "porção", "porcao", "%vd", "valores diários", "valores diarios",
        "informação nutricional", "informacao nutricional", "nutrition facts", "calories", "serving size",
        "ingredientes:", "ingredients:",
    )

    /** Remove ruído e duplicatas, preservando a ordem e o texto original dos nomes que sobram. */
    fun sanitizeNames(names: List<String>): List<String> =
        names.map { it.trim() }
            .filter(::isUsefulName)
            .distinctBy { it.lowercase() }

    fun isUsefulName(name: String): Boolean {
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        return trimmed.length in MIN_LENGTH..MAX_LENGTH &&
            trimmed.any { it.isLetter() } &&
            !codeLike.matches(trimmed) &&
            nutritionTerms.none { lower.contains(it) }
    }
}
