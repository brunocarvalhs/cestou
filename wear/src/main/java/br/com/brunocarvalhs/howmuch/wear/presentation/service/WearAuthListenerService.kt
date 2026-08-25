package br.com.brunocarvalhs.howmuch.wear.presentation.service

import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WearAuthListenerService : WearableListenerService() {

    @Inject
    lateinit var authService: AuthService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/auth/pair") {
            val userId = String(messageEvent.data)
            Timber.tag("WearAuth").d("Received pairing message for user: $userId")
            scope.launch {
                authService.updateUserId(userId)
            }
        }
    }
}
