package br.com.brunocarvalhs.howmuch.core.data.wearable

import android.content.Intent
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for the watch's pairing intent. `RemoteActivityHelper` rejects an `ACTION_VIEW` intent
 * without `BROWSABLE`, which made every pairing fail at once on the watch (sdd-055).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BuildPhonePairingIntentTest {

    @Test
    fun `pairing intent is a BROWSABLE view of the deeplink, addressed to the phone app`() {
        val intent = buildPhonePairingIntent(
            sessionId = "session-1",
            watchPublicKey = "watch-key",
            watchNodeId = "node-1"
        )

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.categories.orEmpty().contains(Intent.CATEGORY_BROWSABLE))
        assertEquals(WearablePaths.PHONE_APPLICATION_ID, intent.`package`)
    }

    @Test
    fun `pairing intent carries the session, the watch key and the watch node in the deeplink`() {
        val data = buildPhonePairingIntent("session-1", "watch-key", "node-1").data

        assertEquals("cestou", data?.scheme)
        assertEquals("wear", data?.host)
        assertEquals("/pair", data?.path)
        assertEquals("session-1", data?.getQueryParameter("sid"))
        assertEquals("watch-key", data?.getQueryParameter("k"))
        assertEquals("node-1", data?.getQueryParameter("n"))
    }
}
