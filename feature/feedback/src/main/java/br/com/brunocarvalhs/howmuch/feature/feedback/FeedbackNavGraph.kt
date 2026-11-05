package br.com.brunocarvalhs.howmuch.feature.feedback

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.FeedbackSurvey
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.StoreReview
import br.com.brunocarvalhs.howmuch.core.navigation.toScreenName
import br.com.brunocarvalhs.howmuch.feature.feedback.navigation.feedbackGraph
import javax.inject.Inject
import kotlin.reflect.KClass

internal class FeedbackNavGraph @Inject constructor() : FeatureNavGraph {

    override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
        FeedbackSurvey::class to AnalyticsScreen.FEEDBACK_SURVEY.toScreenName(),
        StoreReview::class to AnalyticsScreen.STORE_REVIEW.toScreenName()
    )

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.feedbackGraph(navigator)
    }
}
