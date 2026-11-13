package br.com.brunocarvalhs.howmuch.update

import android.content.IntentSender
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceUpdateControllerTest {

    private val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val launcher = mockk<ActivityResultLauncher<IntentSenderRequest>>(relaxed = true)

    private val installed = 20L

    private fun controller() = ForceUpdateController(appUpdateManager, crashReporter)

    /** Makes the Play task complete with [info], which is what kotlinx-coroutines' `await()` reads. */
    private fun playReports(info: AppUpdateInfo) {
        val task = mockk<Task<AppUpdateInfo>>(relaxed = true) {
            every { isComplete } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns info
        }
        every { appUpdateManager.appUpdateInfo } returns task
    }

    private fun info(availability: Int, immediateAllowed: Boolean = true): AppUpdateInfo = mockk {
        every { updateAvailability() } returns availability
        every { isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) } returns immediateAllowed
    }

    @Test
    fun `installed version at or above the minimum is Idle and never asks Play`() {
        val state = runBlocking { controller().check(minVersionCode = installed, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Idle, state)
        verify(exactly = 0) { appUpdateManager.appUpdateInfo }
    }

    @Test
    fun `a zero minimum means no force update and never asks Play`() {
        val state = runBlocking { controller().check(minVersionCode = 0L, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Idle, state)
        verify(exactly = 0) { appUpdateManager.appUpdateInfo }
    }

    @Test
    fun `installed version below the minimum with an immediate update available is Required`() {
        playReports(info(UpdateAvailability.UPDATE_AVAILABLE, immediateAllowed = true))

        val state = runBlocking { controller().check(minVersionCode = 21L, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Required(resume = false), state)
    }

    @Test
    fun `an update already in progress is Required and asks to resume`() {
        playReports(info(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS))

        val state = runBlocking { controller().check(minVersionCode = 21L, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Required(resume = true), state)
    }

    @Test
    fun `below the minimum with no update available from Play is Blocked`() {
        playReports(info(UpdateAvailability.UPDATE_NOT_AVAILABLE))

        val state = runBlocking { controller().check(minVersionCode = 21L, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Blocked, state)
    }

    @Test
    fun `an update that does not allow immediate mode is Blocked`() {
        playReports(info(UpdateAvailability.UPDATE_AVAILABLE, immediateAllowed = false))

        val state = runBlocking { controller().check(minVersionCode = 21L, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Blocked, state)
    }

    @Test
    fun `a Play failure fails open to Idle and is recorded as non-fatal`() {
        every { appUpdateManager.appUpdateInfo } throws mockk<ApiException>(relaxed = true)

        val state = runBlocking { controller().check(minVersionCode = 21L, installedVersionCode = installed) }

        assertEquals(ForceUpdateState.Idle, state)
        verify { crashReporter.recordException(any(), any()) }
    }

    @Test
    fun `starting the update opens the Play flow for the last checked update`() {
        playReports(info(UpdateAvailability.UPDATE_AVAILABLE))
        val forceUpdate = controller()
        runBlocking { forceUpdate.check(minVersionCode = 21L, installedVersionCode = installed) }

        val started = forceUpdate.startImmediateUpdate(launcher)

        assertTrue(started)
        verify { appUpdateManager.startUpdateFlowForResult(any(), launcher, any()) }
    }

    @Test
    fun `a failure to open the Play flow is recorded and reported as not started`() {
        playReports(info(UpdateAvailability.UPDATE_AVAILABLE))
        every {
            appUpdateManager.startUpdateFlowForResult(any(), launcher, any())
        } throws mockk<IntentSender.SendIntentException>(relaxed = true)
        val forceUpdate = controller()
        runBlocking { forceUpdate.check(minVersionCode = 21L, installedVersionCode = installed) }

        val started = forceUpdate.startImmediateUpdate(launcher)

        assertFalse(started)
        verify { crashReporter.recordException(any(), any()) }
    }

    @Test
    fun `starting without a checked update does nothing`() {
        val started = controller().startImmediateUpdate(launcher)

        assertFalse(started)
        verify(exactly = 0) { appUpdateManager.startUpdateFlowForResult(any(), any(), any()) }
    }
}
