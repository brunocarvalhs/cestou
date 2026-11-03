package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import kotlin.reflect.KClass

/**
 * Nome de uma tela para os relatórios de analytics: [id] vira `screen_name` e [screenClass] o
 * nome do Composable (nunca de uma ViewModel).
 */
data class ScreenName(val id: String, val screenClass: String)

interface FeatureNavGraph {

    /**
     * Rotas desta feature e a tela que cada uma representa. Consumido pelo `ScreenTracker` do
     * `:app`, que reporta um `screen_view` a cada mudança de destino (inclui dialogs).
     */
    val screenNames: Map<KClass<*>, ScreenName> get() = emptyMap()

    fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {}

    fun registerWearGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator
    ) {}
}
