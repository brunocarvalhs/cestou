package br.com.brunocarvalhs.howmuch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.Partner
import br.com.brunocarvalhs.howmuch.core.navigation.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.rememberNavigator
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBottomNavigation
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.authGraph
import br.com.brunocarvalhs.howmuch.feature.products.navigation.productsGraph
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.settingsGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.shoppingGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()

            LaunchedEffect(language) {
                val appLocales = LocaleListCompat.forLanguageTags(language)
                AppCompatDelegate.setApplicationLocales(appLocales)
            }

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CestouTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navigator = rememberNavigator(navController)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

                val rootRoutes = listOf(ShoppingList::class, Partner::class, AiChat::class, Profile::class)

                val showBottomBar = currentDestination?.hierarchy?.any { dest ->
                    rootRoutes.any { route -> dest.hasRoute(route) }
                } == true

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            CestouBottomNavigation(
                                currentRoute = null, // TODO: Obter a rota atual como objeto
                                onNavigate = { route ->
                                    navigator.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    Surface(modifier = androidx.compose.ui.Modifier.padding(padding)) {
                        LaunchedEffect(intent) {
                            if (intent?.action == Intent.ACTION_VIEW) {
                                val data = intent.data
                                if (data?.host == "cestou.app" && data.path?.startsWith("/join") == true) {
                                    navigator.navigate(JoinList)
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = if (isAuthenticated) ShoppingList else Welcome
                        ) {
                            authGraph(navigator) {
                                navigator.navigate(ShoppingList) {
                                    popUpTo(Welcome) { inclusive = true }
                                }
                            }
                            shoppingGraph(navigator, windowSizeClass)
                            productsGraph(navigator, windowSizeClass)
                            settingsGraph(navigator)
                        }
                    }
                }
            }
        }
    }
}
