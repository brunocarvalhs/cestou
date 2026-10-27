package br.com.brunocarvalhs.howmuch.core.common.util

/**
 * Parses a persisted "HH:mm" reminder time, falling back to 18:00 for anything empty,
 * legacy or corrupted instead of throwing (crash-hardening sdd-005, C-02).
 */
object ReminderTimeParser {

    private const val DEFAULT_HOUR = 18
    private const val DEFAULT_MINUTE = 0
    private const val MAX_HOUR = 23
    private const val MAX_MINUTE = 59

    fun parse(reminderTime: String): Pair<Int, Int> {
        val parts = reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, MAX_HOUR) ?: DEFAULT_HOUR
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, MAX_MINUTE) ?: DEFAULT_MINUTE
        return hour to minute
    }
}
