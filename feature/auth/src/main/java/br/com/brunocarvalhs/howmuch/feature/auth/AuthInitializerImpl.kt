package br.com.brunocarvalhs.howmuch.feature.auth

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.feature.auth.commons.navigation.Welcome
import br.com.brunocarvalhs.howmuch.feature.auth.commons.navigation.authGraph
import javax.inject.Inject

internal class AuthInitializerImpl @Inject constructor() : AuthInitializer {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.authGraph(navigator) {
            navigator.navigate(ShoppingList) {
                popUpTo(Welcome) { inclusive = true }
            }
        }
    }
}
