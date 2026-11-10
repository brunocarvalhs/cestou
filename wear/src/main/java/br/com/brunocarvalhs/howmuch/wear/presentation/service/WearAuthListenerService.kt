package br.com.brunocarvalhs.howmuch.wear.presentation.service

import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingSessionHolder
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiCodec
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives the phone's pairing messages (sdd-055). It only routes them: the pairing screen decides
 * what they mean. The sign-in is the exception and is guarded here, because it runs even when the
 * screen is gone: it only happens for a session the watch user confirmed.
 */
@AndroidEntryPoint
class WearAuthListenerService : WearableListenerService() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var pairingSessionHolder: PairingSessionHolder

    @Inject
    lateinit var pairingInbox: WearPairingInbox

    @Inject
    lateinit var wearListStore: WearListStore

    @Inject
    lateinit var wearAiInbox: WearAiInbox

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearablePaths.PAIRING_PHONE_KEY -> onPhoneKey(messageEvent)
            WearablePaths.PAIRING_ERROR -> onPairingError(messageEvent)
            WearablePaths.AUTH_PAIR -> onAuthPair(messageEvent)
            WearablePaths.AUTH_SIGN_OUT -> onSignOut()
            WearablePaths.AI_REPLY -> onAiReply(messageEvent)
            WearablePaths.AI_CONFIRM_REQUEST -> onAiConfirmRequest(messageEvent)
            WearablePaths.AI_FAILURE -> onAiFailure(messageEvent)
        }
    }

    private fun onAiReply(messageEvent: MessageEvent) {
        val decoded = WearAiCodec.decodeReply(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            wearAiInbox.emit(WearAiMessage.Reply(decoded.first, decoded.second))
        }
    }

    private fun onAiConfirmRequest(messageEvent: MessageEvent) {
        val decoded = WearAiCodec.decodeConfirmRequest(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            val (requestId, actionId, summary) = decoded
            wearAiInbox.emit(WearAiMessage.ConfirmRequest(requestId, actionId, summary))
        }
    }

    private fun onAiFailure(messageEvent: MessageEvent) {
        val decoded = WearAiCodec.decodeFailure(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            wearAiInbox.emit(WearAiMessage.Failure(decoded.first, decoded.second))
        }
    }

    private fun onPhoneKey(messageEvent: MessageEvent) {
        val decoded = WearablePaths.decodePhoneKey(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            val (sessionId, phoneKey) = decoded
            pairingInbox.emit(WearPairingMessage.PhoneKey(sessionId, phoneKey, messageEvent.sourceNodeId))
        }
    }

    private fun onPairingError(messageEvent: MessageEvent) {
        val decoded = WearablePaths.decodeError(messageEvent.data)
        if (decoded == null) {
            logMalformed(messageEvent)
        } else {
            val (sessionId, reason) = decoded
            pairingInbox.emit(WearPairingMessage.Error(sessionId, reason))
        }
    }

    private fun onAuthPair(messageEvent: MessageEvent) {
        val decoded = WearablePaths.decodeAuthPair(messageEvent.data)
        when {
            decoded == null -> logMalformed(messageEvent)
            !pairingSessionHolder.isConfirmed(decoded.first) ->
                Timber.tag("WearAuth").w("Ignoring sign-in for a session the watch did not confirm")
            else -> signIn(sessionId = decoded.first, userId = decoded.second, idToken = decoded.third)
        }
    }

    private fun signIn(sessionId: String, userId: String, idToken: String) {
        scope.launch {
            authService.signInWithGoogleIdToken(idToken, userId)
                .onFailure {
                    Timber.tag("WearAuth").e(it, "Watch sign-in failed")
                    pairingInbox.emit(WearPairingMessage.Error(sessionId, PairingFailure.SIGN_IN_FAILED.name))
                }
        }
    }

    private fun onSignOut() {
        scope.launch {
            authService.signOut()
            wearListStore.clear()
        }
    }

    private fun logMalformed(messageEvent: MessageEvent) {
        Timber.tag("WearAuth").w("Ignoring a malformed message on %s", messageEvent.path)
    }
}
