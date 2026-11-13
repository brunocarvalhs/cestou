package br.com.brunocarvalhs.howmuch.core.common.wearable

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WearPairingInboxTest {

    @Test
    fun `a subscriber receives what is emitted after it subscribed`() = runTest {
        val inbox = WearPairingInbox()
        val received = mutableListOf<WearPairingMessage>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            inbox.messages.collect { received += it }
        }
        val message = WearPairingMessage.WatchConfirmed("session-1", "node-1")

        inbox.emit(message)

        assertEquals(listOf<WearPairingMessage>(message), received)
        collector.cancel()
    }

    @Test
    fun `a message nobody collects is dropped without blocking`() {
        val inbox = WearPairingInbox()

        inbox.emit(WearPairingMessage.Error("session-1", "TIMEOUT"))

        assertEquals(0, inbox.messages.replayCache.size)
    }
}
