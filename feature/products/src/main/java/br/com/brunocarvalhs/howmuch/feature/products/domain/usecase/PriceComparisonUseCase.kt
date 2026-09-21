package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import javax.inject.Inject

@AiAgentAction(
    id = "compare_prices",
    description = "Compara preços de um produto em diferentes estabelecimentos simulados."
)
@AiAgentParameter(
    name = "productName",
    description = "O nome do produto para comparar",
    isRequired = true
)
class PriceComparisonUseCase @Inject constructor() : AgentActionUseCase<List<Map<String, Any>>>() {

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<List<Map<String, Any>>> {
        val productName = arguments.getString("productName") ?: return Result.failure(Exception("productName missing"))

        // Simulação de dados de preços
        return Result.success(
            listOf(
                mapOf("establishment" to "Supermercado Econômico", "price" to PRICE_ECONOMICO, "unit" to "un"),
                mapOf("establishment" to "Hortifruti da Esquina", "price" to PRICE_HORTIFRUTI, "unit" to "un"),
                mapOf("establishment" to "Atacadão Central", "price" to PRICE_ATACADAO, "unit" to "un")
            )
        )
    }

    private companion object {
        const val PRICE_ECONOMICO = 10.50
        const val PRICE_HORTIFRUTI = 9.90
        const val PRICE_ATACADAO = 8.75
    }
}
