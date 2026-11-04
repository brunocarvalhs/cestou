package br.com.brunocarvalhs.howmuch.feature.chat.data.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import br.com.brunocarvalhs.howmuch.feature.chat.data.notification.AiNotifier
import br.com.brunocarvalhs.howmuch.feature.chat.di.ChatBackgroundScope
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.AiTurnBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AiTurnBackgroundImpl @Inject constructor(
    private val controller: AiTurnServiceController,
    private val notifier: AiNotifier,
    @ChatBackgroundScope private val scope: CoroutineScope
) : AiTurnBackground {

    private val chatVisible = AtomicBoolean(false)

    /** Trocado nos testes; em produção olha o ciclo de vida do processo inteiro. */
    internal var appInForeground: () -> Boolean = {
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    /** O usuário só "está olhando" com o chat composto e o app em primeiro plano. */
    private fun isUserWatching() = chatVisible.get() && appInForeground()

    override fun turnStarted() = controller.turnStarted()

    override fun turnFinished(sessionId: String?, result: String?) {
        controller.turnFinished()
        if (result != null && !isUserWatching()) scope.launch { notifier.postResult(sessionId, result) }
    }

    override fun approvalNeeded(sessionId: String?) {
        if (!isUserWatching()) scope.launch { notifier.postApprovalNeeded(sessionId) }
    }

    override fun setChatVisible(visible: Boolean) {
        chatVisible.set(visible)
        if (visible) notifier.cancelAll()
    }
}
