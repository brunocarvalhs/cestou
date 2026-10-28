package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import com.google.ai.client.generativeai.type.Content

/**
 * Caps [AiSession.history] to a recent sliding window instead of replaying the entire
 * conversation on every request. An unbounded history means every message gets more expensive
 * and slower to send over time, and — as seen with OpenRouter — a bigger payload is more likely
 * to hit a provider-side edge case. The assistant only ever needs recent context to stay
 * coherent; it's not expected to recall a shopping chat from days ago.
 */
internal const val MAX_HISTORY_ENTRIES = 20

/**
 * Corta pelo menos até [maxEntries], mas nunca no meio de um turno: avança do corte ingênuo até
 * o primeiro `role == "user"`, que é sempre onde um turno de fato começa. Cortar por contagem
 * pura (como antes) podia abrir a janela com `role == "model"`/`"tool"`, separando um
 * `functionCall` da sua `functionResponse` — o Gemini rejeita isso com 400 (sdd-006, B-34).
 */
internal fun MutableList<Content>.trimToRecentHistory(maxEntries: Int = MAX_HISTORY_ENTRIES) {
    if (size <= maxEntries) return

    val naiveStart = size - maxEntries
    val safeStart = (naiveStart until size).firstOrNull { index -> this[index].role == "user" } ?: naiveStart
    val trimmed = subList(safeStart, size).toMutableList()
    clear()
    addAll(trimmed)
}
