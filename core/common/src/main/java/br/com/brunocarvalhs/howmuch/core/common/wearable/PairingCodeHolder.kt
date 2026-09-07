package br.com.brunocarvalhs.howmuch.core.common.wearable

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the pairing code the wear app most recently generated, in memory.
 * WearAuthListenerService is started independently by the system to handle
 * an incoming Wearable message and has no direct reference to the
 * PairingViewModel instance that generated the code, so it reads this
 * singleton instead - both live in the same process within the wear app.
 */
@Singleton
class PairingCodeHolder @Inject constructor() {
    @Volatile
    var currentCode: String? = null
}
