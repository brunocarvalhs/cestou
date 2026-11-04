package br.com.brunocarvalhs.howmuch.navigation

import android.app.Application
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.navigation.AiChatLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** sdd-037: o deeplink que a notificação da IA dispara tem ida e volta e rejeita o que não é dele. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class AiChatLaunchTest {

    @Test
    fun `a built link carries the session id back`() {
        val uri = AiChatLaunch.uriFor("3f2c-aa")

        assertEquals("cestou://chat/3f2c-aa", uri.toString())
        assertEquals("3f2c-aa", AiChatLaunch.sessionIdFrom(uri))
    }

    @Test
    fun `other hosts, schemes and shapes are not chat links`() {
        assertNull(AiChatLaunch.sessionIdFrom(Uri.parse("cestou://other/abc")))
        assertNull(AiChatLaunch.sessionIdFrom(Uri.parse("https://chat/abc")))
        assertNull(AiChatLaunch.sessionIdFrom(Uri.parse("cestou://chat")))
        assertNull(AiChatLaunch.sessionIdFrom(Uri.parse("cestou://chat/a/b")))
    }
}
