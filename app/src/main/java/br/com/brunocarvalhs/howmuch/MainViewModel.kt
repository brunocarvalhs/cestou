package br.com.brunocarvalhs.howmuch

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.analytics.AppUsageTracker
import br.com.brunocarvalhs.howmuch.analytics.UserPropertiesSync
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.common.util.InviteLink
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.core.navigation.AiChatLaunch
import br.com.brunocarvalhs.howmuch.deeplink.PendingNewShopping
import br.com.brunocarvalhs.howmuch.deeplink.PendingWearPairing
import br.com.brunocarvalhs.howmuch.deeplink.ShoppingCreateLink
import br.com.brunocarvalhs.howmuch.deeplink.WearPairingLink
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.ChatTurnTracker
import br.com.brunocarvalhs.howmuch.wear.WearSubscriptionSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val KEY_PENDING_JOIN_TOKEN = "pending_join_token"
private const val KEY_PENDING_AI_CHAT = "pending_ai_chat_session"

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val wearableSyncService: WearableSyncService,
    private val aiSession: AiSession,
    private val userPropertiesSync: UserPropertiesSync,
    private val appUsageTracker: AppUsageTracker,
    private val feedbackPromptRequester: FeedbackPromptRequester,
    private val chatTurnTracker: ChatTurnTracker,
    private val wearSubscriptionSync: WearSubscriptionSync
) : ViewModel() {

    init {
        analyticsTracker.trackEvent(AnalyticsEvents.APP_OPEN)
        userPropertiesSync.start(viewModelScope)
        syncUserProfile()
        signOutWatchOnSignOut()
        wearSubscriptionSync.start(viewModelScope)
    }

    /**
     * The watch has its own Firebase session (AD-009); drop it whenever the phone signs out.
     * Also clears [AiSession.history]: it's `@Singleton`, so without this the next account
     * signed in on this device would inherit the previous user's AI chat context (sdd-006, B-11).
     * Pending AI turns are cancelled too: they outlive the chat screen, and a late answer must not
     * land in the next account's history.
     */
    private fun signOutWatchOnSignOut() {
        viewModelScope.launch {
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
    private fun syncUserProfile() {
        viewModelScope.launch {
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

    /** Signed in but without a name: must go through the required-name step (spec EPA-06). */
    val requiresName: StateFlow<Boolean> = authService.authState
        .map { it.requiresName() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser.requiresName()
        )

    /** Synchronous read for navigation decisions, so a name saved a moment ago is never missed. */
    fun requiresNameNow(): Boolean = authService.currentUser.requiresName()

    private fun AuthenticatedUser?.requiresName() = this != null && displayName.isNullOrBlank()

    val isAuthenticated: StateFlow<Boolean> = authService.authState
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser != null
        )
    private val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val themeMode: StateFlow<ThemeMode> = settings
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val language: StateFlow<String> = settings
        .map { it.language }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "pt-BR"
        )

    val photoUrl: StateFlow<String?> = authService.authState
        .map { it?.photoUrl }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser?.photoUrl
        )

    /**
     * An invite token read from a deep link, kept here (not navigated to immediately) until the
     * user is authenticated - see [onDeepLink]. Backed by [SavedStateHandle] so it survives
     * process death/recreation (config change, backgrounding during login) without being lost.
     */
    val pendingJoinToken: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_PENDING_JOIN_TOKEN, null)

    /** Conversa da IA a abrir, vinda do toque numa notificação; consumida uma única vez. */
    val pendingAiChatSession: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_PENDING_AI_CHAT, null)

    /**
     * Pareamento pedido pelo relógio (sdd-055). A tela só abre depois do login; sem login, o
     * celular já respondeu `NOT_SIGNED_IN` (ver [onDeepLink]).
     */
    val pendingWearPairing = PendingWearPairing(savedStateHandle, viewModelScope)

    /** Pedido do relógio para criar lista (sdd-056). Espera o login e o nome, como o pareamento. */
    val pendingNewShopping = PendingNewShopping(savedStateHandle)

    /** A Activity reporta ida e volta do app; a ViewModel decide o que é sessão. */
    fun onForeground() {
        appUsageTracker.onForeground()
        feedbackPromptRequester.onAppForeground()
    }

    fun onBackground() = appUsageTracker.onBackground()

    /** Pedido de pesquisa aguardando um destino estável (sdd-034); o `:app` só o repassa à navegação. */
    val pendingFeedback: StateFlow<FeedbackPrompt?> = feedbackPromptRequester.pending

    /** Chegar na tela de listas é o momento do NPS; a política decide se pergunta. */
    fun onShoppingListReached() = feedbackPromptRequester.onMoment(FeedbackMoment.APP_RELATIONSHIP)

    fun onFeedbackPromptShown(prompt: FeedbackPrompt) = feedbackPromptRequester.consume(prompt)

    /** Reads an invite token out of [intent], if it is one, and holds it for [consumePendingJoinToken]. */
    fun onDeepLink(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        WearPairingLink.parse(data)?.let { request ->
            analyticsTracker.trackEvent(
                AnalyticsEvents.DEEP_LINK_OPENED,
                mapOf(AnalyticsParams.SOURCE to "wear_pairing")
            )
            if (authService.currentUser == null) {
                // Answer now: the watch would otherwise wait for a key that never comes.
                viewModelScope.launch {
                    wearableSyncService.sendPairingError(
                        nodeId = request.wearNodeId,
                        sessionId = request.sessionId,
                        reason = PairingFailure.NOT_SIGNED_IN.name
                    )
                }
            } else {
                pendingWearPairing.store(request)
            }
            return
        }
        if (ShoppingCreateLink.matches(data)) {
            analyticsTracker.trackEvent(
                AnalyticsEvents.DEEP_LINK_OPENED,
                mapOf(AnalyticsParams.SOURCE to "shopping_create")
            )
            pendingNewShopping.store()
            return
        }
        AiChatLaunch.sessionIdFrom(data)?.let {
            analyticsTracker.trackEvent(AnalyticsEvents.DEEP_LINK_OPENED, mapOf(AnalyticsParams.SOURCE to "ai_chat"))
            savedStateHandle[KEY_PENDING_AI_CHAT] = it
            return
        }
        if (!InviteLink.matches(data)) return
        val token = InviteLink.tokenFrom(data) ?: return
        analyticsTracker.trackEvent(AnalyticsEvents.DEEP_LINK_OPENED, mapOf(AnalyticsParams.SOURCE to "invite"))
        savedStateHandle[KEY_PENDING_JOIN_TOKEN] = token
    }

    /**
     * Reads and clears [pendingJoinToken], so a pending invite is only ever processed once.
     * Deliberately `get` + `set(null)`, not `remove()`: [pendingJoinToken] is a [StateFlow]
     * obtained once from [SavedStateHandle.getStateFlow] - `set` updates that same flow's value,
     * while `remove` was verified (by this change's own tests) to leave it stale.
     */
    fun consumePendingAiChatSession(): String? {
        val sessionId = savedStateHandle.get<String>(KEY_PENDING_AI_CHAT)
        savedStateHandle[KEY_PENDING_AI_CHAT] = null
        return sessionId
    }

    fun consumePendingJoinToken(): String? {
        val token = savedStateHandle.get<String>(KEY_PENDING_JOIN_TOKEN)
        savedStateHandle[KEY_PENDING_JOIN_TOKEN] = null
        return token
    }
}
