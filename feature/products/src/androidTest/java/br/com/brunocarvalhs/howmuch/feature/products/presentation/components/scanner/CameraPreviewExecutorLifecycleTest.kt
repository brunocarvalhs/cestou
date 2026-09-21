package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Regression test for G11 (`.specs/MVP-ROADMAP.md`): `CameraPreview` used to create its
 * single-thread analyzer `Executor` via `remember { Executors.newSingleThreadExecutor() }` but
 * never shut it down, so every scanner-screen visit (open -> back -> reopen) leaked one
 * background thread permanently. The fix added a `DisposableEffect(executor) { onDispose {
 * executor.shutdown() } }`.
 *
 * Requires a real device/emulator (`CameraPreview` binds to CameraX, which Robolectric can't
 * exercise meaningfully) — this only asserts the executor lifecycle, not that the camera preview
 * actually renders frames, so it doesn't need the CAMERA runtime permission granted: binding to
 * the camera happens asynchronously inside a try/catch that swallows a missing-permission
 * failure, but the executor is created and disposed regardless of whether that bind succeeds.
 */
@RunWith(AndroidJUnit4::class)
class CameraPreviewExecutorLifecycleTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun disposingCameraPreviewShutsDownTheExecutorItCreated() {
        val trackedExecutors = mutableListOf<ExecutorService>()
        val executorFactory: () -> ExecutorService = {
            Executors.newSingleThreadExecutor().also { trackedExecutors += it }
        }

        var visible by mutableStateOf(true)
        composeTestRule.setContent {
            if (visible) {
                CameraPreview(executorFactory = executorFactory)
            }
        }
        composeTestRule.waitForIdle()

        // Simulate a user opening the scanner screen, leaving it, and reopening it 3 times,
        // all within the same composition (like navigating back and forth on the real screen).
        repeat(3) {
            composeTestRule.runOnUiThread { visible = false }
            composeTestRule.waitForIdle()
            composeTestRule.runOnUiThread { visible = true }
            composeTestRule.waitForIdle()
        }
        // Leave the screen one last time so every created executor has been disposed.
        composeTestRule.runOnUiThread { visible = false }
        composeTestRule.waitForIdle()

        assertTrue(
            "Expected CameraPreview to create one executor per composition (4 mounts total)",
            trackedExecutors.size == 4
        )

        trackedExecutors.forEachIndexed { index, executor ->
            val terminated = executor.awaitTermination(5, TimeUnit.SECONDS)
            assertTrue(
                "Executor #$index created by CameraPreview was not shut down after the " +
                    "composable was disposed - this is the G11 thread leak regressing.",
                terminated && executor.isShutdown
            )
        }
    }
}
