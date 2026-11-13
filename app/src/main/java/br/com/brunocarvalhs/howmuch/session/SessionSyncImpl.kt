package br.com.brunocarvalhs.howmuch.session

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.ChatTurnTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class SessionSyncImpl @Inject constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val wearableSyncService: WearableSyncService,
    private val chatTurnTracker: ChatTurnTracker,
    private val aiSession: AiSession
) : SessionSync {

    override fun start(scope: CoroutineScope) {
        syncUserProfile(scope)
        signOutWatchOnSignOut(scope)
    }

    /**
     * The watch has its own Firebase session (AD-009); drop it whenever the phone signs out.
     * Also clears [AiSession.history]: it's `@Singleton`, so without this the next account
     * signed in on this device would inherit the previous user's AI chat context (sdd-006, B-11).
     * Pending AI turns are cancelled too: they outlive the chat screen, and a late answer must not
     * land in the next account's history.
     */
    private fun signOutWatchOnSignOut(scope: CoroutineScope) {
        scope.launch {
            authService.authState
                .map { it != null }
                .distinctUntilChanged()
                .drop(1)
                .filter { signedIn -> !signedIn }
                .collect {
                    wearableSyncService.notifySignOut()
                    wearableSyncService.syncSubscription(isPro = false)
                    chatTurnTracker.cancelAll()
                    aiSession.clearHistory()
                }
        }
    }

    /**
     * Keeps users/{uid} (name + photo other list members see) in step with the signed-in account,
     * for every sign-in method. Also backfills accounts created before the profile was ever written.
     */
    private fun syncUserProfile(scope: CoroutineScope) {
        scope.launch {
            authService.authState
                .map { user ->
                    user?.takeUnless { it.displayName.isNullOrBlank() }
                        ?.let { UserProfile(id = it.id, name = it.displayName, photoUrl = it.photoUrl) }
                }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { profile ->
                    userRepository.saveProfile(profile)
                        .onFailure { Timber.w(it, "Falha ao sincronizar users/%s", profile.id) }
                }
        }
    }
}
