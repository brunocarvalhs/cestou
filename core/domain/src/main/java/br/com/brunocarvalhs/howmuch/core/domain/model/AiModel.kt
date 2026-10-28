package br.com.brunocarvalhs.howmuch.core.domain.model

/**
 * [vendor] é só o rótulo de exibição na UI (ex.: "NVIDIA", "Google"). [provider] é o provider
 * técnico de roteamento (`"openrouter"` ou `"gemini"`) usado por `AiAgentFactoryImpl` para
 * escolher o SDK correto — antes de existir esse campo, a UI e o roteamento compartilhavam o
 * mesmo valor (o fabricante), e só 1 de 11 modelos roteava certo (sdd-006, B-07).
 */
data class AiModel(
    val id: String,
    val name: String,
    val vendor: String,
    val provider: String = "openrouter",
    val isFree: Boolean = true
) {
    companion object {
        val freeModels = listOf(
            AiModel("openrouter/auto", "Free Models Router", vendor = "OpenRouter"),
            AiModel("nvidia/nemotron-3-ultra", "Nemotron 3 Ultra", vendor = "NVIDIA"),
            AiModel("inclusionai/ling-3.0-flash", "Ling-3.0-flash", vendor = "InclusionAI"),
            AiModel("nvidia/nemotron-3-super", "Nemotron 3 Super", vendor = "NVIDIA"),
            AiModel("cohere/north-mini-code", "North Mini Code", vendor = "Cohere"),
            AiModel("poolside/laguna-s-2.1", "Laguna S 2.1", vendor = "Poolside"),
            AiModel("poolside/laguna-xs-2.1", "Laguna XS 2.1", vendor = "Poolside"),
            AiModel("google/gemma-4-26b-a4b", "Gemma 4 26B A4B", vendor = "Google"),
            AiModel("google/gemma-4-31b", "Gemma 4 31B", vendor = "Google"),
            AiModel("openai/gpt-oss-20b", "GPT OSS 20B", vendor = "OpenAI"),
            AiModel("nvidia/nemotron-3-nano-omni", "Nemotron 3 Nano Omni", vendor = "NVIDIA")
        )
    }
}
