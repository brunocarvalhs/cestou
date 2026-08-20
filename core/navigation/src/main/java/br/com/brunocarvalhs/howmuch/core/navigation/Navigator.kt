package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController

@Stable
class Navigator(val navController: NavController) {
    fun navigate(route: Any, builder: NavOptionsBuilder.() -> Unit = {}) {
        navController.navigate(route, builder)
    }

    fun goBack() {
        navController.popBackStack()
    }
}

@Composable
fun rememberNavigator(navController: NavController = rememberNavController()): Navigator {
    return remember(navController) { Navigator(navController) }
}
