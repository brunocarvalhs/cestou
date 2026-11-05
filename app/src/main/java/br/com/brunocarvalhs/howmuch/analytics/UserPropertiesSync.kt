package br.com.brunocarvalhs.howmuch.analytics

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsUserProperties
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "analytics_user_state"
private const val KEY_EVER_PRO = "ever_pro"
private const val KEY_LAST_PLAN = "last_plan"

/** Tudo que o analytics sabe sobre a pessoa neste instante; nenhum campo carrega dado pessoal. */
private data class Snapshot(
    val user: AuthenticatedUser?,
    val status: SubscriptionStatus,
    val aiTrialUsed: Boolean,
    val settings: AppSettings,
    val listsCount: Int
)

/**
 * Mantém o `user_id` e as user properties do GA4 em dia e aplica o opt-out de Configurações >
 * Dados. Com a coleta desligada, desliga o SDK e apaga identificação e propriedades; ligada,
 * `user_id` é o UID do Firebase Auth (e-mail e nome nunca vão para o Analytics).
 */
@Singleton
internal class UserPropertiesSync @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker,
    private val authService: AuthService,
    private val subscriptionRepository: SubscriptionRepository,
    private val aiTrialRepository: AiTrialRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingRepository: ShoppingRepository,
    private val installSource: InstallSource
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun start(scope: CoroutineScope) {
        applyConsent(scope)
        combine(
            authService.authState,
            subscriptionRepository.observeStatus(),
            aiTrialRepository.hasUsedFreeMessage(),
            settingsRepository.getSettings(),
            shoppingRepository.observeAll().map { it.size }.catch { emit(0) }
        ) { user, status, trial, settings, lists -> Snapshot(user, status, trial, settings, lists) }
            .distinctUntilChanged()
            .onEach(::apply)
            .catch { Timber.w(it, "UserPropertiesSync interrompido") }
            .launchIn(scope)
    }

    /**
     * O opt-out não pode esperar os demais fluxos: o `combine` abaixo só emite depois que auth,
     * assinatura, trial e listas emitirem, e qualquer um deles pode demorar ou nunca emitir
     * (listener do Firestore sem rede). Só [AppSettings.analyticsEnabled] decide a coleta.
     */
    private fun applyConsent(scope: CoroutineScope) {
        settingsRepository.getSettings()
            .map { it.analyticsEnabled }
            .distinctUntilChanged()
            .onEach { enabled ->
                if (!enabled) clearIdentity()
                analyticsTracker.setCollectionEnabled(enabled)
            }
            .catch { Timber.w(it, "Consentimento de analytics interrompido") }
            .launchIn(scope)
    }

    private fun apply(snapshot: Snapshot) {
        if (!snapshot.settings.analyticsEnabled) return
        analyticsTracker.setUserId(snapshot.user?.id)
        if (snapshot.user == null) return

        trackPlanChange(snapshot.status)
        val settings = snapshot.settings
        mapOf(
            AnalyticsUserProperties.PLAN to snapshot.status.planValue(),
            AnalyticsUserProperties.EVER_PRO to everPro(snapshot.status).toString(),
            AnalyticsUserProperties.AI_TRIAL_USED to snapshot.aiTrialUsed.toString(),
            AnalyticsUserProperties.ACCOUNT_AGE_BUCKET to snapshot.user.createdAt?.let(::ageBucket),
            AnalyticsUserProperties.IS_TESTER to installSource.isTester.toString(),
            AnalyticsUserProperties.LISTS_BUCKET to AnalyticsUserProperties.listsBucket(snapshot.listsCount),
            AnalyticsUserProperties.APP_LANGUAGE to settings.language,
            AnalyticsUserProperties.CURRENCY to settings.currency,
            AnalyticsUserProperties.THEME to settings.themeMode.name.lowercase(),
            AnalyticsUserProperties.AI_PROVIDER to settings.aiProvider.name.lowercase(),
            AnalyticsUserProperties.NOTIFICATIONS_ENABLED to settings.notificationsEnabled.toString(),
            AnalyticsUserProperties.REMINDERS_ENABLED to settings.remindersEnabled.toString(),
            AnalyticsUserProperties.SORTING_MODE to settings.sortingMode.lowercase()
        ).forEach { (name, value) -> analyticsTracker.setUserProperty(name, value) }
    }

    private fun clearIdentity() {
        analyticsTracker.setUserId(null)
        listOf(
            AnalyticsUserProperties.PLAN,
            AnalyticsUserProperties.EVER_PRO,
            AnalyticsUserProperties.AI_TRIAL_USED,
            AnalyticsUserProperties.ACCOUNT_AGE_BUCKET,
            AnalyticsUserProperties.IS_TESTER,
            AnalyticsUserProperties.LISTS_BUCKET,
            AnalyticsUserProperties.APP_LANGUAGE,
            AnalyticsUserProperties.CURRENCY,
            AnalyticsUserProperties.THEME,
            AnalyticsUserProperties.AI_PROVIDER,
            AnalyticsUserProperties.NOTIFICATIONS_ENABLED,
            AnalyticsUserProperties.REMINDERS_ENABLED,
            AnalyticsUserProperties.SORTING_MODE,
            AnalyticsUserProperties.NPS_CLASS
        ).forEach { analyticsTracker.setUserProperty(it, null) }
    }

    /** `ever_pro` nunca volta a `false`: separa quem assinou e cancelou de quem nunca pagou. */
    private fun everPro(status: SubscriptionStatus): Boolean {
        val stored = prefs.getBoolean(KEY_EVER_PRO, false)
        if (status == SubscriptionStatus.PRO && !stored) {
            prefs.edit().putBoolean(KEY_EVER_PRO, true).apply()
            return true
        }
        return stored
    }

    private fun trackPlanChange(status: SubscriptionStatus) {
        val current = status.planValue()
        val previous = prefs.getString(KEY_LAST_PLAN, null)
        prefs.edit().putString(KEY_LAST_PLAN, current).apply()
        if (previous != null && previous != current) {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PLAN_CHANGED,
                mapOf(AnalyticsParams.FROM to previous, AnalyticsParams.TO to current)
            )
        }
    }

    private fun ageBucket(createdAtMillis: Long): String =
        AnalyticsUserProperties.accountAgeBucket(
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - createdAtMillis)
        )

    private fun SubscriptionStatus.planValue(): String =
        if (this == SubscriptionStatus.PRO) AnalyticsUserProperties.PLAN_PRO else AnalyticsUserProperties.PLAN_FREE
}
