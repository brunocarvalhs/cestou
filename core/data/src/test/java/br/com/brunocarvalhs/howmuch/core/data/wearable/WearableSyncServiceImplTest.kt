package br.com.brunocarvalhs.howmuch.core.data.wearable

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WearableSyncServiceImplTest {

    private val inbox = WearPairingInbox()
    private val service = WearableSyncServiceImpl(mockk<Context>(relaxed = true), inbox)

    @Test
    fun `observeWatchConfirm delivers the confirmation of its own session once and completes`() = runTest {
        val collected = mutableListOf<WearPairingMessage.WatchConfirmed>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collected += service.observeWatchConfirm("session-1").toList()
        }

        inbox.emit(WearPairingMessage.WatchConfirmed("session-other", "node-x"))
        inbox.emit(WearPairingMessage.WatchConfirmed("session-1", "node-1"))
        inbox.emit(WearPairingMessage.WatchConfirmed("session-1", "node-1"))

        assertEquals(listOf(WearPairingMessage.WatchConfirmed("session-1", "node-1")), collected)
        collector.join()
    }
}
