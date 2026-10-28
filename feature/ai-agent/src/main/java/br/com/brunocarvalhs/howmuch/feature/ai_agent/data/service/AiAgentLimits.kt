package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

/**
 * Teto de rodadas de tool call por turno, compartilhado por [GeminiAiAgent] e
 * [OpenRouterAiAgent]. Sem isso, um modelo gratuito fraco que repete a mesma tool call trava o
 * chat em loop sem fim (sdd-006, B-10).
 */
internal const val MAX_TOOL_ROUNDS = 5

/**
 * Resposta emitida quando [MAX_TOOL_ROUNDS] é atingido sem uma resposta final do modelo.
 *
 * Fica como literal pt-BR (mesma dívida de `SystemPrompts.kt`, rastreada pelo backlog F-07 —
 * `specs/sdd-001-auditoria-geral/tasks.md:90`): `:feature:ai-agent` é um módulo de dados puro,
 * sem `res/` hoje, e esta tarefa (sdd-006) não cria um módulo de recursos Android só por esta
 * string (decisão registrada em `specs/sdd-006-ai-agent-reliability/tasks.md`, §9).
 */
internal const val TOOL_ROUNDS_EXCEEDED_MESSAGE =
    "Desculpe, não consegui concluir sua solicitação depois de várias tentativas. " +
        "Pode tentar de um jeito mais simples?"
