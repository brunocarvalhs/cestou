package br.com.brunocarvalhs.howmuch.feature.feedback.data.review

import android.app.Activity
import android.content.Context
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

private const val REVIEW_TIMEOUT_MS = 10_000L

/** Abre o fluxo de In-App Review; separado para o [PlayReviewLauncher] ser testável sem Play Services. */
internal fun interface ReviewFlow {
    suspend fun run(activity: Activity)
}

internal class PlayReviewFlow @Inject constructor(
    @ApplicationContext private val context: Context
) : ReviewFlow {
    override suspend fun run(activity: Activity) {
        val manager = ReviewManagerFactory.create(context)
        manager.launchReview(activity, manager.requestReview())
    }
}

/**
 * Pede a avaliação na loja. Sem Play Services, com timeout ou qualquer falha, o app segue normal:
 * o erro vai para o [CrashReporter] e [launch] devolve `false`.
 */
@Singleton
internal class PlayReviewLauncher internal constructor(
    private val flow: ReviewFlow,
    private val crashReporter: CrashReporter,
    private val timeoutMs: Long
) {
    @Inject
    constructor(flow: PlayReviewFlow, crashReporter: CrashReporter) : this(flow, crashReporter, REVIEW_TIMEOUT_MS)

    suspend fun launch(activity: Activity): Boolean {
        val failure = runCatching { withTimeout(timeoutMs) { flow.run(activity) } }.exceptionOrNull()
            ?: return true
        if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
        crashReporter.recordException(failure)
        return false
    }
}
