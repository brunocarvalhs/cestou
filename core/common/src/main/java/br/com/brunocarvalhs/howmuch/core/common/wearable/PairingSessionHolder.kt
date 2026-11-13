package br.com.brunocarvalhs.howmuch.core.common.wearable

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watch-side state of the one pairing session in progress. WearAuthListenerService runs
 * independently of the pairing screen, so it asks this singleton whether an incoming
 * sign-in belongs to the session the user confirmed on the watch.
 *
 * Holds only ids and a flag, never keys or tokens.
 */
@Singleton
class PairingSessionHolder @Inject constructor() {
    @Volatile
    var activeSessionId: String? = null
        private set

    @Volatile
    private var watchConfirmed = false

    @Synchronized
    fun startSession(sessionId: String) {
        activeSessionId = sessionId
        watchConfirmed = false
    }

    /** Marks the watch user's "Parear" tap. Returns false when [sessionId] is not the active session. */
    @Synchronized
    fun markWatchConfirmed(sessionId: String): Boolean {
        if (sessionId != activeSessionId) return false
        watchConfirmed = true
        return true
    }

    /** True only for the active session after the watch user confirmed it. */
    @Synchronized
    fun isConfirmed(sessionId: String): Boolean = watchConfirmed && sessionId == activeSessionId

    @Synchronized
    fun endSession() {
        activeSessionId = null
        watchConfirmed = false
    }
}
