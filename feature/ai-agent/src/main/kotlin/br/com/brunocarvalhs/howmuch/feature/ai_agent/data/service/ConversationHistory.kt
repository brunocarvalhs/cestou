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

internal fun MutableList<Content>.trimToRecentHistory(maxEntries: Int = MAX_HISTORY_ENTRIES) {
    val overflow = size - maxEntries
    if (overflow > 0) {
        repeat(overflow) { removeAt(0) }
    }
}
