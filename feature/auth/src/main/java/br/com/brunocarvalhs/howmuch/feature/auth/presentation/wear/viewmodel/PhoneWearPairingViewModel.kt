package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.auth.domain.wear.PairingHandshake
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui.GoogleIdTokenProvider
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingStage
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingUiState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyPair
import javax.inject.Inject

/**
 * Phone side of the pairing (sdd-055). Opened by the watch's deeplink with the session id, the
 * watch's public key and the watch node id.
 *
 * The sign-in token leaves the phone only after both users confirmed: the phone's "Parear" and the
 * watch's confirmation. A session that stalls ends with [PairingFailure.TIMEOUT] instead of waiting.
 */
@HiltViewModel
internal class PhoneWearPairingViewModel @Inject constructor(
    private val wearableSyncService: WearableSyncService,
    private val authService: AuthService,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val featureFlagService: FeatureFlagService,
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneWearPairingUiState())
    val uiState = _uiState.asStateFlow()

    private var session: CoroutineScope? = null
    private var sessionId: String? = null
    private var watchNodeId: String? = null

    /** Sign-in material, held only between the phone's "Parear" and the send. Cleared right after. */
    private var pendingAuth: PendingAuth? = null
    private var authSent = false

    /**
     * Starts the session for the deeplink. Idempotent for the same [sessionId], so a recomposition
     * or a configuration change does not restart the pairing.
     */
    fun start(sessionId: String, watchPublicKey: String, watchNodeId: String) {
        if (this.sessionId == sessionId) return
        endSession()
        this.sessionId = sessionId
        this.watchNodeId = watchNodeId
        _uiState.value = PhoneWearPairingUiState(stage = PhoneWearPairingStage.CONNECTING)

        val scope = newSession()
        session = scope
        scope.launch {
            if (!featureFlagService.isEnabled(FeatureFlagKeys.WEAR_LOGIN_ENABLED, default = true)) {
                return@launch fail(PairingFailure.DISABLED)
            }
            if (authService.currentUser == null) return@launch fail(PairingFailure.NOT_SIGNED_IN)
            if (subscriptionRepository.observeStatus().first() != SubscriptionStatus.PRO) {
                return@launch fail(PairingFailure.NOT_PRO)
            }
            val watchKey = PairingHandshake.decodePublicKey(watchPublicKey)
                ?: return@launch fail(PairingFailure.INVALID_KEY)

            val pair = withContext(Dispatchers.Default) { PairingHandshake.newKeyPair() }
            val phoneKey = pair.public.encoded
            val code = withContext(Dispatchers.Default) { deriveCode(pair, watchKey, phoneKey) }
                ?: return@launch fail(PairingFailure.INVALID_KEY)

            // Subscribe before answering, so the watch's confirmation cannot slip past the listener.
            val confirmations = wearableSyncService.observeWatchConfirm(sessionId)
            launch(start = CoroutineStart.UNDISPATCHED) {
                confirmations.collect { onWatchConfirmed() }
            }

            _uiState.update { it.copy(stage = PhoneWearPairingStage.CODE, code = code) }
            val sent = wearableSyncService.sendPhoneKey(watchNodeId, sessionId, PairingHandshake.encodePublicKey(pair))
            if (!sent) return@launch fail(PairingFailure.PHONE_UNREACHABLE, notifyWatch = false)

            analyticsTracker.trackEvent(AnalyticsEvents.WEAR_PAIR_CONFIRM)
            launch {
                delay(SESSION_TIMEOUT_MS)
                fail(PairingFailure.TIMEOUT, onlyIf = PhoneWearPairingStage.CODE)
            }
        }
    }

    /**
     * The phone user tapped "Parear". Checks the Google account the watch will sign in with, then
     * waits for the watch's confirmation (with a timeout) before anything is sent.
     */
    fun confirm(context: Context) {
        val state = _uiState.value
        val target = activeTarget()
        if (state.stage != PhoneWearPairingStage.CODE || state.isPhoneConfirmed || target == null) return
        val currentSessionId = target.sessionId
        val nodeId = target.nodeId

        target.scope.launch {
            val user = authService.currentUser
            val credential = googleIdTokenProvider.get(context)
            when {
                user == null || credential == null -> fail(PairingFailure.NO_GOOGLE_ACCOUNT)
                !credential.id.equals(user.email, ignoreCase = true) -> fail(PairingFailure.ACCOUNT_MISMATCH)
                else -> {
                    pendingAuth = PendingAuth(userId = user.id, googleIdToken = credential.idToken)
                    _uiState.update { it.copy(isPhoneConfirmed = true) }
                    launch {
                        delay(WATCH_CONFIRM_TIMEOUT_MS)
                        if (!_uiState.value.isWatchConfirmed) {
                            fail(PairingFailure.TIMEOUT, onlyIf = PhoneWearPairingStage.CODE)
                        }
                    }
                    sendAuthIfReady(currentSessionId, nodeId)
                }
            }
        }
    }

    /** Leaves the pairing and tells the watch. Safe to call more than once. */
    fun cancel() {
        val currentSessionId = sessionId
        val nodeId = watchNodeId
        val state = _uiState.value
        val finished = state.stage == PhoneWearPairingStage.DONE || state.stage == PhoneWearPairingStage.ERROR
        endSession()
        if (!finished && currentSessionId != null && nodeId != null) {
            viewModelScope.launch {
                wearableSyncService.sendPairingError(nodeId, currentSessionId, PairingFailure.CANCELED.name)
            }
        }
    }

    private fun onWatchConfirmed() {
        if (_uiState.value.stage != PhoneWearPairingStage.CODE) return
        _uiState.update { it.copy(isWatchConfirmed = true) }
        val currentSessionId = sessionId
        val nodeId = watchNodeId
        if (currentSessionId != null && nodeId != null) sendAuthIfReady(currentSessionId, nodeId)
    }

    /** Sends the sign-in material once, and only when both sides confirmed. */
    private fun sendAuthIfReady(currentSessionId: String, nodeId: String) {
        val state = _uiState.value
        val auth = pendingAuth ?: return
        if (!state.isPhoneConfirmed || !state.isWatchConfirmed || authSent) return
        authSent = true
        pendingAuth = null
        _uiState.update { it.copy(stage = PhoneWearPairingStage.SYNCING) }

        viewModelScope.launch {
            val sent = wearableSyncService.sendAuthPair(nodeId, currentSessionId, auth.userId, auth.googleIdToken)
            if (sent) {
                session?.cancel()
                _uiState.update { it.copy(stage = PhoneWearPairingStage.DONE) }
                analyticsTracker.trackEvent(AnalyticsEvents.WEAR_PAIR_SUCCESS)
            } else {
                fail(PairingFailure.PHONE_UNREACHABLE, notifyWatch = false)
            }
        }
    }

    /**
     * Ends the session with [failure]. [onlyIf] keeps a late timer from overriding a state that has
     * already moved on (for example, a timeout firing after the user confirmed).
     */
    private fun fail(
        failure: PairingFailure,
        notifyWatch: Boolean = true,
        onlyIf: PhoneWearPairingStage? = null
    ) {
        val state = _uiState.value
        if (state.stage == PhoneWearPairingStage.DONE) return
        if (onlyIf != null && state.stage != onlyIf) return
        val currentSessionId = sessionId
        val nodeId = watchNodeId
        endSession()
        _uiState.value = PhoneWearPairingUiState(stage = PhoneWearPairingStage.ERROR, failure = failure)
        analyticsTracker.trackEvent(AnalyticsEvents.WEAR_PAIR_ERROR, mapOf(AnalyticsParams.REASON to failure.name))
        if (notifyWatch && currentSessionId != null && nodeId != null) {
            viewModelScope.launch {
                wearableSyncService.sendPairingError(nodeId, currentSessionId, failure.name)
            }
        }
    }

    private fun deriveCode(pair: KeyPair, watchKey: ByteArray, phoneKey: ByteArray): String? = try {
        val secret = PairingHandshake.sharedSecret(pair.private, watchKey)
        PairingHandshake.deriveCode(secret, watchKey, phoneKey)
    } catch (_: java.security.GeneralSecurityException) {
        null
    }

    private fun endSession() {
        session?.cancel()
        session = null
        sessionId = null
        watchNodeId = null
        pendingAuth = null
        authSent = false
    }

    /** The running session with everything a reply needs, or null when there is none yet. */
    private fun activeTarget(): ActiveTarget? {
        val scope = session
        val id = sessionId
        val node = watchNodeId
        return if (scope != null && id != null && node != null) ActiveTarget(scope, id, node) else null
    }

    private class ActiveTarget(val scope: CoroutineScope, val sessionId: String, val nodeId: String)

    private fun newSession(): CoroutineScope =
        CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))

    override fun onCleared() {
        endSession()
        super.onCleared()
    }

    private class PendingAuth(val userId: String, val googleIdToken: String)

    private companion object {
        /** How long the phone waits, from showing the code, for the phone user to tap "Parear". */
        const val SESSION_TIMEOUT_MS = 120_000L

        /** How long the phone waits for the watch's confirmation after its own "Parear". */
        const val WATCH_CONFIRM_TIMEOUT_MS = 60_000L
    }
}
