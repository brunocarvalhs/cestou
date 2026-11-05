package br.com.brunocarvalhs.howmuch.notifications

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** sdd-037: o pedido de permissão acontece uma vez por instalação e só no Android 13+. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class NotificationPermissionRequesterTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `asks when the permission is missing and was never asked`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue(NotificationPermissionRequester(context).shouldAsk())
    }

    @Test
    fun `does not ask again after the first request`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val requester = NotificationPermissionRequester(context)

        requester.markAsked()

        assertFalse(requester.shouldAsk())
    }

    @Test
    fun `does not ask when the permission is already granted`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertFalse(NotificationPermissionRequester(context).shouldAsk())
    }

    @Test
    fun `never asks before Android 13`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertFalse(NotificationPermissionRequester(context, sdkInt = Build.VERSION_CODES.S_V2).shouldAsk())
    }
}
