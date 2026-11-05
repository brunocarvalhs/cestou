package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController
import timber.log.Timber

@Stable
class Navigator {
    private var _navController: NavController? = null

    /**
     * Acesso nulo-seguro ao [NavController] atualmente vinculado. Use em pontos que podem ser
     * compostos antes do primeiro [bind] (ex.: primeiro frame após restauração de processo) e
     * que sabem lidar com "ainda não vinculado" sem crashar.
     */
    val navControllerOrNull: NavController?
        get() = _navController

    val navController: NavController
        get() = _navController ?: throw IllegalStateException("Navigator not bound to a NavController")

    fun bind(navController: NavController) {
        _navController = navController
    }

    /**
     * Desfaz o vínculo, mas só se [navController] ainda for a MESMA instância atualmente
     * vinculada. [Navigator] é um singleton do Hilt que sobrevive à recriação da Activity
     * (rotação, dark mode, troca de idioma, "não manter atividades" etc.), então o `onDispose`
     * da Activity/composição antiga pode, em tese, disparar depois que uma Activity nova já
     * chamou [bind] com seu próprio NavController. Sem essa checagem de identidade, esse
     * `onDispose` tardio apagaria o bind novo e válido, fazendo qualquer acesso seguinte a
     * [navController] lançar `Navigator not bound to a NavController` (crash reportado no
     * Crashlytics) mesmo com a Activity atual plenamente vinculada.
     */
    fun unbind(navController: NavController) {
        if (_navController === navController) {
            _navController = null
        }
    }

    /**
     * Tolerates calls that land in the window where this [Navigator] isn't bound yet (e.g. a
     * recreated Activity's first frame) or no longer bound (the unbind race described on
     * [unbind]): logging and returning beats crashing on `Navigator not bound to a
     * NavController`, a fatal seen on Crashlytics for exactly this kind of timing gap.
     */
    fun navigate(route: Any, builder: NavOptionsBuilder.() -> Unit = {}) {
        val navController = navControllerOrNull ?: run {
            /**
             * Logs the route's type, never route.toString(): some routes (e.g. JoinList) carry an
             * invite token in a field, and AGENTS.md §5 forbids logging tokens/personal data.
             */
            Timber.w("Navigator.navigate(%s) ignored: not bound to a NavController", route::class.simpleName)
            return
        }
        if (route is String) {
            navController.navigate(route, builder)
        } else {
            navController.navigate(route, builder)
        }
    }

    fun goBack() {
        val navController = navControllerOrNull ?: run {
            Timber.w("Navigator.goBack() ignored: not bound to a NavController")
            return
        }
        navController.popBackStack()
    }
}

@Composable
fun rememberNavigator(navController: NavController = rememberNavController()): Navigator {
    return remember(navController) {
        Navigator().apply { bind(navController) }
    }
}
