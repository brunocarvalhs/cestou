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
 * The watch's "open on phone" intent (sdd-056). Same shape as the pairing intent: `RemoteActivityHelper`
 * needs `BROWSABLE` and an addressed package, or the phone never starts the activity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BuildPhoneAppIntentTest {

    @Test
    fun `open intent is a BROWSABLE view addressed to the phone app`() {
        val intent = buildPhoneAppIntent("shopping/new")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.categories.orEmpty().contains(Intent.CATEGORY_BROWSABLE))
        assertEquals(WearablePaths.PHONE_APPLICATION_ID, intent.`package`)
    }

    @Test
    fun `open intent carries the requested path in the cestou scheme`() {
        assertEquals("cestou://shopping/new", buildPhoneAppIntent("shopping/new").dataString)
        assertEquals("cestou://home", buildPhoneAppIntent("home").dataString)
    }
}
