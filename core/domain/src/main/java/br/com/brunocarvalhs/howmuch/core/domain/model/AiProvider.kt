package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Provider técnico de roteamento do agente de IA. `AUTO` representa "nenhuma escolha explícita"
 * (hoje inatingível pela leitura real do DataStore — ver `SettingsRepositoryImpl.resolveStoredAiProvider`;
 * sdd-026 preserva esse comportamento, não o corrige).
 */
@Serializable
enum class AiProvider { AUTO, GEMINI, OPENROUTER }
