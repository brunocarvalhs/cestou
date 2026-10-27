package br.com.brunocarvalhs.howmuch.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeParserTest {

    @Test
    fun `parse returns the hour and minute for a valid time`() {
        assertEquals(VALID_HOUR to VALID_MINUTE, ReminderTimeParser.parse("18:30"))
    }

    @Test
    fun `parse falls back to 18-00 for an empty string`() {
        assertEquals(DEFAULT_HOUR to DEFAULT_MINUTE, ReminderTimeParser.parse(""))
    }

    @Test
    fun `parse falls back to 18-00 for a non-numeric time`() {
        assertEquals(DEFAULT_HOUR to DEFAULT_MINUTE, ReminderTimeParser.parse("abc:def"))
    }

    @Test
    fun `parse coerces out-of-range values instead of throwing`() {
        assertEquals(MAX_HOUR to MAX_MINUTE, ReminderTimeParser.parse("99:99"))
    }

    @Test
    fun `parse falls back to 18-00 when the minute part is missing`() {
        assertEquals(HOUR_ONLY to DEFAULT_MINUTE, ReminderTimeParser.parse("9"))
    }
}

private const val VALID_HOUR = 18
private const val VALID_MINUTE = 30
private const val DEFAULT_HOUR = 18
private const val DEFAULT_MINUTE = 0
private const val MAX_HOUR = 23
private const val MAX_MINUTE = 59
private const val HOUR_ONLY = 9
