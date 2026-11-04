package br.com.brunocarvalhs.howmuch.feature.chat.data.service

import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTurnTrackerImplTest {

    private val tracker = ChatTurnTrackerImpl()

    @Test
    fun `begin and end track each conversation independently`() {
        tracker.begin("a", Job())
        tracker.begin("b", Job())
        assertEquals(setOf("a", "b"), tracker.inFlight.value)

        tracker.end("a")
        assertEquals(setOf("b"), tracker.inFlight.value)
    }

    @Test
    fun `end without begin is a no-op`() {
        tracker.end("missing")

        assertTrue(tracker.inFlight.value.isEmpty())
    }

    @Test
    fun `cancelAll cancels every job and empties the tracker`() {
        val a = Job()
        val b = Job()
        tracker.begin("a", a)
        tracker.begin("b", b)

        tracker.cancelAll()

        assertTrue(a.isCancelled && b.isCancelled)
        assertTrue(tracker.inFlight.value.isEmpty())
    }
}
