package br.com.brunocarvalhs.howmuch.core.data.wearable

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

/**
 * B-2 of the sdd-055 review (R-7): the session id and the sign-in token must never reach Timber.
 * Drives the send paths that fail, and checks everything they log, message and exception included.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WearableSyncLogRedactionTest {

    private val logged = mutableListOf<String>()
    private val tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logged += message
            t?.let { logged += listOf(it.message.orEmpty(), it.stackTraceToString()) }
        }
    }

    @Before
    fun setUp() {
        Timber.plant(tree)
        mockkStatic(Wearable::class)
        val messageClient = mockk<MessageClient>()
        every { Wearable.getMessageClient(any<Context>()) } returns messageClient
        every { messageClient.sendMessage(any(), any(), any()) } returns
            Tasks.forException(ApiException(Status(CommonStatusCodes.ERROR)))
    }

    @After
    fun tearDown() {
        Timber.uproot(tree)
        unmockkStatic(Wearable::class)
    }

    @Test
    fun `failed sends log the paths and errors, never the session id or the sign-in token`() = runBlocking {
        val service = WearableSyncServiceImpl(mockk<Context>(relaxed = true), WearPairingInbox())

        val authSent = service.sendAuthPair(NODE, SESSION_ID, USER_ID, SIGN_IN_TOKEN)
        service.sendPairingError(NODE, SESSION_ID, "CANCELED")

        assertFalse(authSent)
        val output = logged.joinToString("\n")
        assertTrue("the test must reach the logging path", output.contains("/auth/pair"))
        assertFalse(output.contains(SESSION_ID))
        assertFalse(output.contains(SIGN_IN_TOKEN))
        assertFalse(output.contains(USER_ID))
    }

    private companion object {
        const val NODE = "node-1"
        const val SESSION_ID = "c2Vzc2lvbi1zZWNyZXQtMTIz"
        const val USER_ID = "user-123"
        const val SIGN_IN_TOKEN = "eyJhbGciOiJSUzI1NiJ9.token-secret"
    }
}
