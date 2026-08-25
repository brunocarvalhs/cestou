package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController

@Stable
class Navigator {
    private var _navController: NavController? = null
    val navController: NavController
        get() = _navController ?: throw IllegalStateException("Navigator not bound to a NavController")

    fun bind(navController: NavController) {
        _navController = navController
    }

    fun navigate(route: Any, builder: NavOptionsBuilder.() -> Unit = {}) {
        if (route is String) {
            navController.navigate(route, builder)
        } else {
            navController.navigate(route, builder)
        }
    }

    fun goBack() {
        navController.popBackStack()
    }
}

@Composable
fun rememberNavigator(navController: NavController = rememberNavController()): Navigator {
    return remember(navController) { 
        Navigator().apply { bind(navController) } 
    }
}
