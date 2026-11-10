package br.com.brunocarvalhs.howmuch

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.analytics.AppUsageTracker
import br.com.brunocarvalhs.howmuch.analytics.UserPropertiesSync
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.util.InviteLink
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.ChatTurnTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val wearableSyncService = mockk<WearableSyncService>(relaxed = true)
    private val userRepository = mockk<UserRepository> {
        coEvery { saveProfile(any()) } returns Result.success(Unit)
    }
    private val aiSession = mockk<AiSession>(relaxed = true)
    private val userPropertiesSync = mockk<UserPropertiesSync>(relaxed = true)
    private val appUsageTracker = mockk<AppUsageTracker>(relaxed = true)
    private val chatTurnTracker = mockk<ChatTurnTracker>(relaxed = true)
    private val feedbackPending = MutableStateFlow<FeedbackPrompt?>(null)
    private val feedbackPromptRequester = mockk<FeedbackPromptRequester>(relaxed = true) {
        every { pending } returns feedbackPending
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        settings: AppSettings = AppSettings(),
        currentUser: AuthenticatedUser? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): MainViewModel {
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.getSettings() } returns flowOf(settings)
        authState.value = currentUser
        every { authService.authState } returns authState
        every { authService.currentUser } answers { authState.value }
        return MainViewModel(
            savedStateHandle,
            settingsRepository,
            authService,
            userRepository,
            analyticsTracker,
            wearableSyncService,
            aiSession,
            userPropertiesSync,
            appUsageTracker,
            feedbackPromptRequester,
            chatTurnTracker
        )
    }

    private val authState = MutableStateFlow<AuthenticatedUser?>(null)

    @Test
    fun `signing out on the phone signs the watch out too`() {
        viewModel(currentUser = AuthenticatedUser(id = "user-1", displayName = "Bruno"))

        authState.value = null

        coVerify(exactly = 1) { wearableSyncService.notifySignOut() }
    }

    @Test
    fun `starting signed out does not message the watch`() {
        viewModel(currentUser = null)

        coVerify(exactly = 0) { wearableSyncService.notifySignOut() }
    }

    @Test
    fun `signing out clears the shared AiSession history, so the next account doesn't inherit it`() {
        viewModel(currentUser = AuthenticatedUser(id = "user-1", displayName = "Bruno"))

        authState.value = null

        verify(exactly = 1) { aiSession.clearHistory() }
    }

    @Test
    fun `signing out cancels the pending AI turns, so a late answer never lands in the next account`() {
        viewModel(currentUser = AuthenticatedUser(id = "user-1", displayName = "Bruno"))

        authState.value = null

        verify(exactly = 1) { chatTurnTracker.cancelAll() }
    }

    @Test
    fun `starting signed out does not touch the AiSession history`() {
        viewModel(currentUser = null)

        verify(exactly = 0) { aiSession.clearHistory() }
    }

    @Test
    fun `init tracks app_open`() {
        viewModel()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.APP_OPEN) }
    }

    @Test
    fun `isAuthenticated is false when there is no current user`() = runTest {
        val vm = viewModel(currentUser = null)

        vm.isAuthenticated.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isAuthenticated is true when there is a current user`() = runTest {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1"))

        vm.isAuthenticated.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `themeMode and language reflect the current settings`() = runTest {
        val vm = viewModel(settings = AppSettings(themeMode = ThemeMode.DARK, language = "en"))

        vm.themeMode.test { assertEquals(ThemeMode.DARK, awaitItem()) }
        vm.language.test { assertEquals("en", awaitItem()) }
    }

    @Test
    fun `photoUrl reflects the current user's photo`() = runTest {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1", photoUrl = "http://x/y.png"))

        vm.photoUrl.test { assertEquals("http://x/y.png", awaitItem()) }
    }

    // region required name (spec EPA-06/07) and profile sync

    @Test
    fun `requiresName is true for a signed-in user without a display name`() = runTest {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = " "))

        vm.requiresName.test { assertTrue(awaitItem()) }
        assertTrue(vm.requiresNameNow())
    }

    @Test
    fun `requiresName is false for a named user and for a signed-out user`() = runTest {
        val named = viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = "Ana Silva"))
        named.requiresName.test { assertFalse(awaitItem()) }
        assertFalse(named.requiresNameNow())

        val signedOut = viewModel(currentUser = null)
        signedOut.requiresName.test { assertFalse(awaitItem()) }
        assertFalse(signedOut.requiresNameNow())
    }

    @Test
    fun `requiresNameNow reads the latest auth state, not a stale flow value`() {
        val vm = viewModel(currentUser = AuthenticatedUser(id = "u1"))

        authState.value = AuthenticatedUser(id = "u1", displayName = "Ana Silva")

        assertFalse(vm.requiresNameNow())
    }

    @Test
    fun `saves the users profile for a signed-in user with a name, without the e-mail`() {
        viewModel(
            currentUser = AuthenticatedUser(
                id = "u1",
                email = "ana@test.com",
                displayName = "Ana Silva",
                photoUrl = "http://x/y.png"
            )
        )

        coVerify(exactly = 1) {
            userRepository.saveProfile(UserProfile(id = "u1", name = "Ana Silva", photoUrl = "http://x/y.png"))
        }
    }

    @Test
    fun `saves the profile again only when name or photo change`() {
        viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = "Ana Silva"))

        authState.value = AuthenticatedUser(id = "u1", displayName = "Ana Silva", email = "new@test.com")
        authState.value = AuthenticatedUser(id = "u1", displayName = "Ana Souza")

        coVerify(exactly = 2) { userRepository.saveProfile(any()) }
        coVerify { userRepository.saveProfile(UserProfile(id = "u1", name = "Ana Souza")) }
    }

    @Test
    fun `does not save a profile without a name or without a user`() {
        viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = null))
        authState.value = null

        coVerify(exactly = 0) { userRepository.saveProfile(any()) }
    }

    @Test
    fun `a failed profile save does not crash the app`() {
        coEvery { userRepository.saveProfile(any()) } returns Result.failure(IllegalStateException("offline"))

        viewModel(currentUser = AuthenticatedUser(id = "u1", displayName = "Ana Silva"))
    }

    // endregion

    // region deep link join token (sdd-004-navigator-deeplink)

    private fun inviteIntent(token: String?, path: List<String> = listOf("join", token.orEmpty())): Intent {
        val uri = mockk<Uri>(relaxed = true) {
            every { host } returns InviteLink.HOST
            every { pathSegments } returns path
            every { getQueryParameter("token") } returns null
        }
        return mockk {
            every { action } returns Intent.ACTION_VIEW
            every { data } returns uri
        }
    }

    private fun chatIntent(sessionId: String?): Intent {
        val uri = mockk<Uri>(relaxed = true) {
            every { scheme } returns "cestou"
            every { host } returns "chat"
            every { pathSegments } returns listOfNotNull(sessionId)
        }
        return mockk {
            every { action } returns Intent.ACTION_VIEW
            every { data } returns uri
        }
    }

    @Test
    fun `onDeepLink keeps the AI chat session from a notification tap`() {
        val vm = viewModel()

        vm.onDeepLink(chatIntent("session-1"))

        assertEquals("session-1", vm.pendingAiChatSession.value)
    }

    @Test
    fun `the AI chat session is consumed only once`() {
        val vm = viewModel()
        vm.onDeepLink(chatIntent("session-1"))

        assertEquals("session-1", vm.consumePendingAiChatSession())
        assertNull(vm.consumePendingAiChatSession())
        assertNull(vm.pendingAiChatSession.value)
    }

    @Test
    fun `a chat link without an id leaves no pending AI chat`() {
        val vm = viewModel()

        vm.onDeepLink(chatIntent(null))

        assertNull(vm.pendingAiChatSession.value)
    }

    @Test
    fun `onDeepLink stores the token from a matching invite link`() {
        val vm = viewModel()

        vm.onDeepLink(inviteIntent(token = "ABC123"))

        assertEquals("ABC123", vm.pendingJoinToken.value)
    }

    @Test
    fun `onDeepLink ignores an intent that is not a VIEW action`() {
        val vm = viewModel()
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_MAIN
            every { data } returns null
        }

        vm.onDeepLink(intent)

        assertNull(vm.pendingJoinToken.value)
    }

    @Test
    fun `onDeepLink ignores a VIEW intent for an unrelated link`() {
        val vm = viewModel()
        val uri = mockk<Uri>(relaxed = true) {
            every { host } returns "example.com"
            every { pathSegments } returns listOf("other")
        }
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_VIEW
            every { data } returns uri
        }

        vm.onDeepLink(intent)

        assertNull(vm.pendingJoinToken.value)
    }

    @Test
    fun `consumePendingJoinToken returns the stored token once, then null`() {
        val vm = viewModel()
        vm.onDeepLink(inviteIntent(token = "ABC123"))

        assertEquals("ABC123", vm.consumePendingJoinToken())
        assertNull(vm.consumePendingJoinToken())
        assertNull(vm.pendingJoinToken.value)
    }

    @Test
    fun `pendingJoinToken survives across ViewModel instances sharing a SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val first = viewModel(savedStateHandle = savedStateHandle)
        first.onDeepLink(inviteIntent(token = "ABC123"))

        val recreated = viewModel(savedStateHandle = savedStateHandle)

        assertEquals("ABC123", recreated.pendingJoinToken.value)
    }

    // endregion

    @Test
    fun `starting the app starts the user properties sync`() {
        viewModel(currentUser = null)

        verify { userPropertiesSync.start(any()) }
    }

    @Test
    fun `foreground and background are forwarded to the usage tracker`() {
        val vm = viewModel(currentUser = null)

        vm.onForeground()
        vm.onBackground()

        verify { appUsageTracker.onForeground() }
        verify { appUsageTracker.onBackground() }
        verify { feedbackPromptRequester.onAppForeground() }
    }

    @Test
    fun `reaching the shopping list asks the policy for the relationship survey`() {
        val vm = viewModel(currentUser = null)

        vm.onShoppingListReached()

        verify { feedbackPromptRequester.onMoment(FeedbackMoment.APP_RELATIONSHIP) }
    }

    @Test
    fun `pending feedback is exposed and consumed through the requester`() {
        val vm = viewModel(currentUser = null)
        val prompt = FeedbackPrompt(SurveyKind.NPS)
        feedbackPending.value = prompt

        assertEquals(prompt, vm.pendingFeedback.value)
        vm.onFeedbackPromptShown(prompt)

        verify { feedbackPromptRequester.consume(prompt) }
    }
}
