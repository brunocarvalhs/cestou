package br.com.brunocarvalhs.howmuch.analytics

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class UserPropertiesSyncTest {

    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val user = MutableStateFlow<AuthenticatedUser?>(
        AuthenticatedUser(id = "uid-1", email = "ana@test.com", displayName = "Ana", createdAt = null)
    )
    private val status = MutableStateFlow(SubscriptionStatus.FREE)
    private val settings = MutableStateFlow(AppSettings())
    private val store = mutableMapOf<String, Any>()
    private val installSource = mockk<InstallSource> { every { isTester } returns false }

    private fun start(statusFlow: Flow<SubscriptionStatus> = status) {
        val sync = UserPropertiesSync(
            context = contextWithPrefs(store),
            analyticsTracker = analytics,
            authService = mockk<AuthService> { every { authState } returns user },
            subscriptionRepository = mockk<SubscriptionRepository> { every { observeStatus() } returns statusFlow },
            aiTrialRepository = mockk<AiTrialRepository> { every { hasUsedFreeMessage() } returns flowOf(true) },
            settingsRepository = mockk<SettingsRepository> { every { getSettings() } returns settings },
            shoppingRepository = mockk<ShoppingRepository> { every { observeAll() } returns flowOf(emptyList()) },
            installSource = installSource
        )
        sync.start(TestScope(UnconfinedTestDispatcher()))
    }

    @Test
    fun `identifies the user by the auth uid and never sends the email or name`() {
        start()

        verify { analytics.setUserId("uid-1") }
        verify(exactly = 0) { analytics.setUserProperty(any(), "ana@test.com") }
        verify(exactly = 0) { analytics.setUserProperty(any(), "Ana") }
    }

    @Test
    fun `shopping toggles are reported as user properties`() {
        settings.value = AppSettings(remindersEnabled = true, sortingMode = "CATEGORY")

        start()

        verify { analytics.setUserProperty("reminders_enabled", "true") }
        verify { analytics.setUserProperty("sorting_mode", "category") }
        verify { analytics.setUserProperty("notifications_enabled", "true") }
    }

    @Test
    fun `a free account is reported as plan free and never pro`() {
        start()

        verify { analytics.setUserProperty("plan", "free") }
        verify { analytics.setUserProperty("ever_pro", "false") }
        verify { analytics.setUserProperty("ai_trial_used", "true") }
    }

    @Test
    fun `upgrading reports plan pro, ever_pro true and a plan_changed event`() {
        start()

        status.value = SubscriptionStatus.PRO

        verify { analytics.setUserProperty("plan", "pro") }
        verify { analytics.setUserProperty("ever_pro", "true") }
        verify { analytics.trackEvent(AnalyticsEvents.PLAN_CHANGED, mapOf("from" to "free", "to" to "pro")) }
    }

    @Test
    fun `ever_pro stays true after the subscription ends`() {
        start()
        status.value = SubscriptionStatus.PRO

        status.value = SubscriptionStatus.FREE

        verify { analytics.setUserProperty("plan", "free") }
        verify(atLeast = 1) { analytics.setUserProperty("ever_pro", "true") }
    }

    @Test
    fun `account age is bucketed from the creation date`() {
        user.value = user.value?.copy(createdAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3))

        start()

        verify { analytics.setUserProperty("account_age_bucket", "2-7d") }
    }

    @Test
    fun `non play store installs are flagged as tester`() {
        every { installSource.isTester } returns true

        start()

        verify { analytics.setUserProperty("is_tester", "true") }
    }

    @Test
    fun `opting out disables collection and clears the identity`() {
        start()

        settings.value = AppSettings(analyticsEnabled = false)

        verify { analytics.setCollectionEnabled(false) }
        verify { analytics.setUserId(null) }
        verify { analytics.setUserProperty("plan", null) }
        verify { analytics.setUserProperty("nps_class", null) }
    }

    @Test
    fun `signed out clears the user id`() {
        start()

        user.value = null

        verify { analytics.setUserId(null) }
    }

    @Test
    fun `the opt-out is applied even when the other flows never emit`() {
        settings.value = AppSettings(analyticsEnabled = false)

        start(statusFlow = emptyFlow())

        verify { analytics.setCollectionEnabled(false) }
        verify { analytics.setUserId(null) }
    }

    @Test
    fun `an opted-out user is never identified again`() {
        settings.value = AppSettings(analyticsEnabled = false)

        start()
        user.value = AuthenticatedUser(id = "uid-2", email = "b@test.com", displayName = "B", createdAt = null)

        verify(exactly = 0) { analytics.setUserId("uid-2") }
    }
}
