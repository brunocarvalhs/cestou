package br.com.brunocarvalhs.howmuch.feature.chat.data.service

import br.com.brunocarvalhs.howmuch.feature.chat.data.notification.AiNotifier
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

/** sdd-037: só notifica quando o usuário não está olhando o chat. */
@OptIn(ExperimentalCoroutinesApi::class)
class AiTurnBackgroundImplTest {

    private val controller = mockk<AiTurnServiceController>(relaxed = true)
    private val notifier = mockk<AiNotifier>(relaxed = true)
    private val background = AiTurnBackgroundImpl(
        controller,
        notifier,
        CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
    )

    private fun inForeground(value: Boolean) {
        background.appInForeground = { value }
    }

    @Test
    fun `a started turn turns the foreground service on`() {
        background.turnStarted()

        verify(exactly = 1) { controller.turnStarted() }
    }

    @Test
    fun `a finished turn always releases the service`() {
        background.turnFinished("s1", null)

        verify(exactly = 1) { controller.turnFinished() }
    }

    @Test
    fun `the result is posted when the chat is closed`() {
        inForeground(true)
        background.setChatVisible(false)

        background.turnFinished("s1", "Pronto! Criei a lista.")

        coVerify(exactly = 1) { notifier.postResult("s1", "Pronto! Criei a lista.") }
    }

    @Test
    fun `the result is posted when the app is in the background even with the chat open`() {
        inForeground(false)
        background.setChatVisible(true)

        background.turnFinished("s1", "Pronto!")

        coVerify(exactly = 1) { notifier.postResult("s1", "Pronto!") }
    }

    @Test
    fun `nothing is posted while the user is looking at the chat`() {
        inForeground(true)
        background.setChatVisible(true)

        background.turnFinished("s1", "Pronto!")

        coVerify(exactly = 0) { notifier.postResult(any(), any()) }
    }

    @Test
    fun `a cancelled turn never posts a result`() {
        inForeground(false)
        background.setChatVisible(false)

        background.turnFinished("s1", null)

        coVerify(exactly = 0) { notifier.postResult(any(), any()) }
    }

    @Test
    fun `an approval request notifies only out of sight`() {
        inForeground(true)
        background.setChatVisible(true)
        background.approvalNeeded("s1")
        coVerify(exactly = 0) { notifier.postApprovalNeeded(any()) }

        background.setChatVisible(false)
        background.approvalNeeded("s1")

        coVerify(exactly = 1) { notifier.postApprovalNeeded("s1") }
    }

    @Test
    fun `opening the chat clears the pending notifications`() {
        background.setChatVisible(true)

        verify(exactly = 1) { notifier.cancelAll() }
    }

    @Test
    fun `closing the chat does not clear notifications`() {
        background.setChatVisible(false)

        verify(exactly = 0) { notifier.cancelAll() }
    }
}
