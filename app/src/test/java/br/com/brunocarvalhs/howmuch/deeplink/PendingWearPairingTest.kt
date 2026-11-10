package br.com.brunocarvalhs.howmuch.deeplink

import androidx.lifecycle.SavedStateHandle
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.WearPairing
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingWearPairingTest {

    private val pairing = WearPairing(sessionId = "session-1", wearPublicKey = "key", wearNodeId = "node-1")

    @Test
    fun `nothing is pending until a request is stored`() = runTest(UnconfinedTestDispatcher()) {
        val pending = PendingWearPairing(SavedStateHandle(), backgroundScope)

        assertNull(pending.request.value)
    }

    @Test
    fun `a stored request survives a new instance on the same handle`() = runTest(UnconfinedTestDispatcher()) {
        val handle = SavedStateHandle()
        PendingWearPairing(handle, backgroundScope).store(pairing)

        val recreated = PendingWearPairing(handle, backgroundScope)

        assertEquals(pairing, recreated.request.value)
    }

    @Test
    fun `consuming clears the request`() = runTest(UnconfinedTestDispatcher()) {
        val pending = PendingWearPairing(SavedStateHandle(), backgroundScope)
        pending.store(pairing)

        pending.consume()

        assertNull(pending.request.value)
    }
}
