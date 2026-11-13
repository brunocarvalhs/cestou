package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingSessionHolder
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.auth.domain.wear.PairingHandshake
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingStage
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyPair
import javax.inject.Inject

/**
 * Watch side of the pairing (sdd-055). One session at a time:
 * start -> the phone answers with its key -> both show the same code -> the user taps "Parear" ->
 * the phone sends the sign-in material -> the watch signs in (WearAuthListenerService).
 *
 * Every wait has a timeout, so a session never waits forever and [start] is the only retry path.
 */
@HiltViewModel
internal class PairingViewModel @Inject constructor(
    private val wearableSyncService: WearableSyncService,
    private val wearPairingInbox: WearPairingInbox,
    private val pairingSessionHolder: PairingSessionHolder,
    private val authService: AuthService,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState = _uiState.asStateFlow()

    private var session: CoroutineScope? = null
    private var sessionId: String? = null
    private var keyPair: KeyPair? = null
    private var watchPublicKey: ByteArray? = null
    private var phoneNodeId: String? = null

    init {
        observeSignIn()
    }

    /** Starts a new session. No-op while one is already running, so recomposition cannot restart it. */
    fun start() {
        val stage = _uiState.value.stage
        if (stage == PairingStage.CONNECTING || stage == PairingStage.CODE || stage == PairingStage.SYNCING) return

        endSession()
        val newSessionId = PairingHandshake.newSessionId()
        sessionId = newSessionId
        pairingSessionHolder.startSession(newSessionId)
        _uiState.value = PairingUiState(stage = PairingStage.CONNECTING)
        analyticsTracker.trackEvent(AnalyticsEvents.WEAR_PAIR_START)

        val scope = newSession()
        session = scope

        // Subscribe before anything else: the key generation below suspends, and the phone's answer
        // must not be missed while it runs. UNDISPATCHED makes the subscription happen right here.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            wearPairingInbox.messages
                .filter { it.sessionId == newSessionId }
                .collect { handle(it) }
        }

        scope.launch {
            val pair = withContext(Dispatchers.Default) { PairingHandshake.newKeyPair() }
            keyPair = pair
            watchPublicKey = pair.public.encoded

            val requested = wearableSyncService.requestPhonePairing(
                sessionId = newSessionId,
                watchPublicKey = PairingHandshake.encodePublicKey(pair)
            )
            if (!requested) return@launch fail(PairingFailure.PHONE_UNREACHABLE, notifyPhone = false)

            launch {
                delay(PHONE_KEY_TIMEOUT_MS)
                fail(PairingFailure.TIMEOUT, notifyPhone = false, onlyIf = PairingStage.CONNECTING)
            }
            launch {
                delay(SESSION_TIMEOUT_MS)
                fail(PairingFailure.TIMEOUT, onlyIf = null)
            }
        }
    }

    /** The user tapped "Parear" on the watch. Only valid once the code is shown. */
    fun confirm() {
        val state = _uiState.value
        val target = activeTarget()
        if (state.stage != PairingStage.CODE || state.isWatchConfirmed || target == null) return
        val currentSessionId = target.sessionId
        val nodeId = target.nodeId

        target.scope.launch {
            // Mark before sending: the phone's answer can arrive before sendWearConfirm returns.
            pairingSessionHolder.markWatchConfirmed(currentSessionId)
            _uiState.update { it.copy(isWatchConfirmed = true, stage = PairingStage.SYNCING) }
            val sent = wearableSyncService.sendWearConfirm(nodeId, currentSessionId)
            if (!sent) fail(PairingFailure.PHONE_UNREACHABLE, notifyPhone = false)
        }
    }

    /** Leaves the pairing: stops the session and tells the phone. Safe to call more than once. */
    fun cancel() {
        val currentSessionId = sessionId
        val nodeId = phoneNodeId
        val wasLinking = _uiState.value.stage != PairingStage.IDLE && !_uiState.value.isLinked
        endSession()
        if (wasLinking && currentSessionId != null && nodeId != null) {
            viewModelScope.launch {
                wearableSyncService.sendPairingError(nodeId, currentSessionId, PairingFailure.CANCELED.name)
            }
        }
        _uiState.value = PairingUiState()
    }

    private suspend fun handle(message: WearPairingMessage) {
        when (message) {
            is WearPairingMessage.PhoneKey -> onPhoneKey(message)
            is WearPairingMessage.Error -> {
                val failure = PairingFailure.fromWire(message.reason) ?: return
                fail(failure, notifyPhone = false)
            }
            is WearPairingMessage.WatchConfirmed -> Unit // The watch never receives this one.
        }
    }

    private suspend fun onPhoneKey(message: WearPairingMessage.PhoneKey) {
        if (_uiState.value.stage != PairingStage.CONNECTING) return
        val pair = keyPair
        val watchKey = watchPublicKey
        val phoneKey = PairingHandshake.decodePublicKey(message.phonePublicKey)
        if (pair == null || watchKey == null || phoneKey == null) {
            return fail(PairingFailure.INVALID_KEY, notifyPhone = false)
        }

        val code = withContext(Dispatchers.Default) {
            val secret = PairingHandshake.sharedSecret(pair.private, phoneKey)
            PairingHandshake.deriveCode(secret, watchKey, phoneKey)
        }
        phoneNodeId = message.sourceNodeId
        _uiState.update { it.copy(stage = PairingStage.CODE, code = code) }
    }

    /**
     * Ends the session with [failure]. [onlyIf] keeps a late timer from overriding a state that
     * already moved on; null means "whatever the state is".
     */
    private fun fail(
        failure: PairingFailure,
        notifyPhone: Boolean = true,
        onlyIf: PairingStage? = null
    ) {
        val state = _uiState.value
        if (state.isLinked) return
        if (onlyIf != null && state.stage != onlyIf) return
        val currentSessionId = sessionId
        val nodeId = phoneNodeId
        endSession()
        _uiState.value = PairingUiState(stage = PairingStage.ERROR, failure = failure)
        analyticsTracker.trackEvent(AnalyticsEvents.WEAR_PAIR_ERROR, mapOf(AnalyticsParams.REASON to failure.name))
        if (notifyPhone && currentSessionId != null && nodeId != null) {
            viewModelScope.launch {
                wearableSyncService.sendPairingError(nodeId, currentSessionId, failure.name)
            }
        }
    }

    /** The phone's sign-in (WearAuthListenerService) lands here: the user is signed in on the watch. */
    private fun observeSignIn() {
        viewModelScope.launch {
            authService.authState
                .filterNotNull()
                .collect {
                    if (_uiState.value.stage != PairingStage.SYNCING) return@collect
                    endSession()
                    _uiState.update { it.copy(isLinked = true) }
                    analyticsTracker.trackEvent(AnalyticsEvents.WEAR_PAIR_SUCCESS)
                }
        }
    }

    private fun endSession() {
        session?.cancel()
        session = null
        sessionId = null
        keyPair = null
        watchPublicKey = null
        phoneNodeId = null
        pairingSessionHolder.endSession()
    }

    /** The running session with everything a reply needs, or null when there is none yet. */
    private fun activeTarget(): ActiveTarget? {
        val scope = session
        val id = sessionId
        val node = phoneNodeId
        return if (scope != null && id != null && node != null) ActiveTarget(scope, id, node) else null
    }

    private class ActiveTarget(val scope: CoroutineScope, val sessionId: String, val nodeId: String)

    private fun newSession(): CoroutineScope =
        CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))

    override fun onCleared() {
        endSession()
        super.onCleared()
    }

    private companion object {
        /** How long the watch waits for the phone's key after asking for the pairing. */
        const val PHONE_KEY_TIMEOUT_MS = 30_000L

        /** Upper bound for the whole session, so a user who walks away never leaves it pending. */
        const val SESSION_TIMEOUT_MS = 120_000L
    }
}
