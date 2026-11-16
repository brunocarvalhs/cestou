package br.com.brunocarvalhs.howmuch.feature.settings.data.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingReminderWorkerTest {

    private val today = 20_000L

    @Test
    fun `reminds when a list is open and the app was not opened today`() {
        assertTrue(ShoppingReminderWorker.shouldNotify(pendingCount = 1, lastOpenDay = today - 1, today = today))
    }

    @Test
    fun `reminds when the last open day is unknown`() {
        assertTrue(ShoppingReminderWorker.shouldNotify(pendingCount = 2, lastOpenDay = -1L, today = today))
    }

    @Test
    fun `does not remind when the app was already opened today`() {
        assertFalse(ShoppingReminderWorker.shouldNotify(pendingCount = 3, lastOpenDay = today, today = today))
    }

    @Test
    fun `does not remind without an open list`() {
        assertFalse(ShoppingReminderWorker.shouldNotify(pendingCount = 0, lastOpenDay = today - 5, today = today))
    }
}
