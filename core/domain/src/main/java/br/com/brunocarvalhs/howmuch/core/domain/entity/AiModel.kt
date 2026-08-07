package br.com.brunocarvalhs.howmuch.core.domain.entity

data class AiModel(
    val id: String,
    val name: String,
    val provider: String,
    val isFree: Boolean = true
) {
    companion object {
        val freeModels = listOf(
            AiModel("openrouter/auto", "Free Models Router", "OpenRouter"),
            AiModel("nvidia/nemotron-3-ultra", "Nemotron 3 Ultra", "NVIDIA"),
            AiModel("inclusionai/ling-3.0-flash", "Ling-3.0-flash", "InclusionAI"),
            AiModel("nvidia/nemotron-3-super", "Nemotron 3 Super", "NVIDIA"),
            AiModel("cohere/north-mini-code", "North Mini Code", "Cohere"),
            AiModel("poolside/laguna-s-2.1", "Laguna S 2.1", "Poolside"),
            AiModel("poolside/laguna-xs-2.1", "Laguna XS 2.1", "Poolside"),
            AiModel("google/gemma-4-26b-a4b", "Gemma 4 26B A4B", "Google"),
            AiModel("google/gemma-4-31b", "Gemma 4 31B", "Google"),
            AiModel("openai/gpt-oss-20b", "GPT OSS 20B", "OpenAI"),
            AiModel("nvidia/nemotron-3-nano-omni", "Nemotron 3 Nano Omni", "NVIDIA")
        )
    }
}
