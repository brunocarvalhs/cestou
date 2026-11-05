package br.com.brunocarvalhs.howmuch.core.domain.model

/**
 * [vendor] é só o rótulo de exibição na UI (ex.: "NVIDIA", "Google"). [provider] é o provider
 * técnico de roteamento ([AiProvider.OPENROUTER] ou [AiProvider.GEMINI], sdd-026) usado por
 * `AgentFactoryImpl` para escolher o SDK correto — antes de existir esse campo, a UI e o
 * roteamento compartilhavam o mesmo valor (o fabricante), e só 1 de 11 modelos roteava certo
 * (sdd-006, B-07).
 */
data class AiModel(
    val id: String,
    val name: String,
    val vendor: String,
    val provider: AiProvider = AiProvider.OPENROUTER,
    val isFree: Boolean = true
)
