package br.com.brunocarvalhs.howmuch

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.analytics.AppUsageTracker
import br.com.brunocarvalhs.howmuch.analytics.UserPropertiesSync
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.deeplink.DeepLinkAction
import br.com.brunocarvalhs.howmuch.deeplink.DeepLinkParser
import br.com.brunocarvalhs.howmuch.deeplink.PendingNewShopping
import br.com.brunocarvalhs.howmuch.deeplink.PendingPaywall
import br.com.brunocarvalhs.howmuch.deeplink.PendingWearPairing
import br.com.brunocarvalhs.howmuch.session.SessionSync
import br.com.brunocarvalhs.howmuch.wear.WearSubscriptionSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_PENDING_JOIN_TOKEN = "pending_join_token"
private const val KEY_PENDING_AI_CHAT = "pending_ai_chat_session"

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    private val authService: AuthService,
    analyticsTracker: AnalyticsTracker,
    private val wearableSyncService: WearableSyncService,
    userPropertiesSync: UserPropertiesSync,
    private val appUsageTracker: AppUsageTracker,
    private val feedbackPromptRequester: FeedbackPromptRequester,
    wearSubscriptionSync: WearSubscriptionSync,
    sessionSync: SessionSync,
    private val deepLinkParser: DeepLinkParser
) : ViewModel() {

    init {
        analyticsTracker.trackEvent(AnalyticsEvents.APP_OPEN)
        userPropertiesSync.start(viewModelScope)
        sessionSync.start(viewModelScope)
        wearSubscriptionSync.start(viewModelScope)
    }

    val requiresName: StateFlow<Boolean> = authService.authState
        .map { it.requiresName() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser.requiresName()
        )

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

    val pendingJoinToken: StateFlow<String?> =
        savedStateHandle.getStateFlow(KEY_PENDING_JOIN_TOKEN, null)

    val pendingAiChatSession: StateFlow<String?> =
        savedStateHandle.getStateFlow(KEY_PENDING_AI_CHAT, null)

    val pendingWearPairing = PendingWearPairing(savedStateHandle, viewModelScope)

    val pendingNewShopping = PendingNewShopping(savedStateHandle)

    val pendingPaywall = PendingPaywall(savedStateHandle)

    fun onForeground() {
        appUsageTracker.onForeground()
        feedbackPromptRequester.onAppForeground()
    }

    fun onBackground() = appUsageTracker.onBackground()

    val pendingFeedback: StateFlow<FeedbackPrompt?> = feedbackPromptRequester.pending

    fun onShoppingListReached() = feedbackPromptRequester.onMoment(FeedbackMoment.APP_RELATIONSHIP)

    fun onFeedbackPromptShown(prompt: FeedbackPrompt) = feedbackPromptRequester.consume(prompt)

    fun onDeepLink(intent: Intent) {
        when (val action = deepLinkParser.parse(intent)) {
            is DeepLinkAction.PairWatch -> if (authService.currentUser == null) {
                // Answer now: the watch would otherwise wait for a key that never comes.
                viewModelScope.launch {
                    wearableSyncService.sendPairingError(
                        nodeId = action.request.wearNodeId,
                        sessionId = action.request.sessionId,
                        reason = PairingFailure.NOT_SIGNED_IN.name
                    )
                }
            } else {
                pendingWearPairing.store(action.request)
            }

            DeepLinkAction.ShoppingCreate -> pendingNewShopping.store()
            DeepLinkAction.Subscription -> pendingPaywall.store()
            is DeepLinkAction.AiChat -> savedStateHandle[KEY_PENDING_AI_CHAT] = action.sessionId
            is DeepLinkAction.Invite -> savedStateHandle[KEY_PENDING_JOIN_TOKEN] = action.token
            DeepLinkAction.Ignored -> Unit
        }
    }

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
