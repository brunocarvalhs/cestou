package br.com.brunocarvalhs.howmuch.analytics

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

private const val MAX_VISITED = 50

/**
 * Reporta um `screen_view` a cada mudança de destino, no lugar das ViewModels (que mandavam o
 * próprio nome como `screen_class`, e só uma vez, no `init`). O mapa rota -> tela é montado uma
 * vez a partir dos [FeatureNavGraph.screenNames] de todas as features.
 */
@Singleton
internal class ScreenTracker internal constructor(
    private val analyticsTracker: AnalyticsTracker,
    featureNavGraphs: Set<FeatureNavGraph>,
    private val matches: (NavDestination, KClass<*>) -> Boolean
) {
    @Inject
    constructor(
        analyticsTracker: AnalyticsTracker,
        featureNavGraphs: Set<@JvmSuppressWildcards FeatureNavGraph>
    ) : this(analyticsTracker, featureNavGraphs, { destination, route -> destination.hasRoute(route) })

    private val screens: List<Pair<KClass<*>, ScreenName>> =
        featureNavGraphs.flatMap { it.screenNames.entries }.map { it.key to it.value }

    private var lastEntryId: String? = null

    /** Pilha de (entrada, tela) já reportadas; voltar a uma entrada que já está nela é um "voltar". */
    private val visited = ArrayDeque<Pair<String, String>>()
    private var tabSwitchPending = false
    private var currentScreenId: String? = null

    /**
     * Troca de aba da barra inferior. Marca a próxima mudança de destino como troca de aba, pois o
     * `restoreState` devolve entradas antigas que não são um "voltar" do usuário.
     */
    fun onTabSelected(route: Any) {
        val screen = screens.firstOrNull { (kclass, _) -> kclass == route::class }?.second ?: return
        // Reselecionar a aba atual não muda de destino; marcar aqui engoliria o próximo "voltar" real.
        tabSwitchPending = screen.id != currentScreenId
        analyticsTracker.trackEvent(AnalyticsEvents.TAB_SELECTED, mapOf(AnalyticsParams.TO to screen.id))
    }

    /** [entryId] evita reportar de novo a mesma entrada em recomposição ou rotação. */
    fun onDestinationChanged(entryId: String, destination: NavDestination) {
        if (entryId == lastEntryId) return
        val screen = screens.firstOrNull { (route, _) -> matches(destination, route) }?.second
        if (screen == null) {
            Timber.w("Destino sem screenName para analytics: %s", destination.route)
            return
        }
        lastEntryId = entryId
        recordBackIfAny(entryId, screen)
        analyticsTracker.trackScreenView(screenName = screen.id, screenClass = screen.screenClass)
    }

    /** Voltar revela onde o cliente hesita ou desiste: de qual tela saiu e para qual voltou. */
    private fun recordBackIfAny(entryId: String, screen: ScreenName) {
        val index = visited.indexOfFirst { it.first == entryId }
        val isBack = index >= 0 && !tabSwitchPending
        if (isBack) {
            val from = visited.last().second
            analyticsTracker.trackEvent(
                AnalyticsEvents.BACK_PRESSED,
                mapOf(AnalyticsParams.FROM to from, AnalyticsParams.TO to screen.id)
            )
        }
        tabSwitchPending = false
        currentScreenId = screen.id
        if (index >= 0) {
            while (visited.size > index) visited.removeLast()
        }
        visited.addLast(entryId to screen.id)
        // Trocar de aba devolve entradas novas sem nunca desempilhar as antigas: o limite evita crescer a sessão toda.
        if (visited.size > MAX_VISITED) visited.removeFirst()
    }
}
