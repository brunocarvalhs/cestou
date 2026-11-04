package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.coordinator

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.feature.feedback.data.config.FeedbackConfigSource
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.PromptContext
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.policy.FeedbackPolicy
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val SESSION_GAP_MILLIS = TimeUnit.MINUTES.toMillis(30)

/**
 * Recebe os momentos de valor das features e decide, pela [FeedbackPolicy], se publica um pedido em
 * [pending]. Quem mostra é o `:app` (num destino estável); aqui só se decide. Uma pesquisa por
 * processo: [shownThisSession] não é zerado.
 */
@Singleton
internal class FeedbackPromptCoordinator internal constructor(
    private val store: FeedbackStateStore,
    private val analyticsTracker: AnalyticsTracker,
    private val configSource: FeedbackConfigSource,
    private val authService: AuthService,
    private val clock: () -> Long
) : FeedbackPromptRequester {

    @Inject
    constructor(
        store: FeedbackStateStore,
        analyticsTracker: AnalyticsTracker,
        configSource: FeedbackConfigSource,
        authService: AuthService
    ) : this(store, analyticsTracker, configSource, authService, System::currentTimeMillis)

    private val _pending = MutableStateFlow<FeedbackPrompt?>(null)
    override val pending: StateFlow<FeedbackPrompt?> = _pending.asStateFlow()

    private var pendingMoment: FeedbackMoment? = null
    private var shownThisSession = false
    private var lastForegroundAt = 0L

    override fun onAppForeground() {
        val now = clock()
        if (lastForegroundAt == 0L || now - lastForegroundAt > SESSION_GAP_MILLIS) store.recordSession()
        lastForegroundAt = now
    }

    override fun onMoment(moment: FeedbackMoment) {
        // Conta a compra mesmo com a coleta desligada: o marco da avaliação na loja não pode ficar subestimado.
        if (moment == FeedbackMoment.PURCHASE_FINISHED) store.recordPurchaseFinished()
        if (!isFeedbackEnabled()) return
        if (_pending.value != null) return
        val policy = FeedbackPolicy(configSource.current(), clock)
        val state = store.load()
        val ctx = context()
        val prompt = policy.evaluate(moment, state, ctx)
            ?: FeedbackPrompt(SurveyKind.STORE_REVIEW).takeIf {
                moment == FeedbackMoment.PURCHASE_FINISHED && policy.shouldRequestStoreReview(state, ctx)
            }
        if (prompt != null) {
            pendingMoment = moment
            _pending.value = prompt
        }
    }

    override fun consume(prompt: FeedbackPrompt) {
        if (_pending.value != prompt) return
        _pending.value = null
        val trigger = pendingMoment?.name?.lowercase().orEmpty()
        pendingMoment = null
        if (prompt.kind == SurveyKind.STORE_REVIEW) {
            store.recordStoreReviewRequested()
            analyticsTracker.trackEvent(
                AnalyticsEvents.STORE_REVIEW_REQUESTED,
                mapOf(AnalyticsParams.TRIGGER to trigger)
            )
            return
        }
        shownThisSession = true
        store.recordShown(prompt)
        analyticsTracker.trackEvent(
            AnalyticsEvents.FEEDBACK_PROMPT_SHOWN,
            mapOf(
                AnalyticsParams.KIND to prompt.kind.name.lowercase(),
                AnalyticsParams.FEATURE to prompt.feature.orEmpty(),
                AnalyticsParams.TRIGGER to trigger
            )
        )
    }

    override fun isFeedbackEnabled(): Boolean =
        analyticsTracker.isCollectionEnabled && configSource.current().enabled

    private fun context() = PromptContext(
        analyticsEnabled = analyticsTracker.isCollectionEnabled,
        accountAgeDays = authService.currentUser?.createdAt?.let { TimeUnit.MILLISECONDS.toDays(clock() - it) },
        shownThisSession = shownThisSession
    )
}
