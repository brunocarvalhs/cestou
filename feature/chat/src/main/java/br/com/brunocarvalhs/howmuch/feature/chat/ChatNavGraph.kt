package br.com.brunocarvalhs.howmuch.feature.chat

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.toScreenName
import br.com.brunocarvalhs.howmuch.feature.chat.navigation.AiHistory
import br.com.brunocarvalhs.howmuch.feature.chat.navigation.chatGraph
import javax.inject.Inject
import kotlin.reflect.KClass

internal class ChatNavGraph @Inject constructor() : FeatureNavGraph {

    override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
        AiChat::class to AnalyticsScreen.AI_CHAT.toScreenName(),
        AiHistory::class to AnalyticsScreen.AI_CHAT_HISTORY.toScreenName()
    )

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.chatGraph(navigator)
    }
}
