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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBottomNavigation
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var featureInitializers: Set<@JvmSuppressWildcards FeatureInitializer>

    @Inject
    lateinit var navigator: Navigator

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CestouApp(windowSizeClass = calculateWindowSizeClass(this))
        }
    }

    @Composable
    private fun CestouApp(windowSizeClass: WindowSizeClass) {
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val language by viewModel.language.collectAsStateWithLifecycle()
        val photoUrl by viewModel.photoUrl.collectAsStateWithLifecycle()

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

            LaunchedEffect(navController) {
                navigator.bind(navController)
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()

            // Dialog destinations (androidx.navigation `dialog<...>`) render as an overlay on
            // top of the current screen without replacing it, so they must not affect which
            // screen drives the bottom bar — otherwise it collapses/reflows behind the dialog.
            var screenBackStackEntry by remember { mutableStateOf(navBackStackEntry) }
            LaunchedEffect(navBackStackEntry) {
                val destination = navBackStackEntry?.destination
                if (destination != null && destination !is DialogNavigator.Destination) {
                    screenBackStackEntry = navBackStackEntry
                }
            }
            val currentDestination = screenBackStackEntry?.destination
            val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

            var wasAuthenticated by remember { mutableStateOf(isAuthenticated) }
            LaunchedEffect(isAuthenticated) {
                if (isAuthenticated) {
                    wasAuthenticated = true
                } else if (wasAuthenticated) {
                    wasAuthenticated = false
                    navigator.navigate(Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            val rootRoutes = remember { listOf(ShoppingList, AiChat, Profile) }

            val currentRoute = rootRoutes.find { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            }

            val showBottomBar = currentRoute != null

            Scaffold(
                bottomBar = {
                    CestouBottomNavigation(
                        currentRoute = currentRoute,
                        photoUrl = photoUrl,
                        visible = showBottomBar,
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
                },
            ) { padding ->
                Surface(modifier = Modifier.padding(padding)) {
                    LaunchedEffect(intent) {
                        if (intent?.action == Intent.ACTION_VIEW) {
                            val data = intent.data
                            if (data?.host == "cestou.app" && data.path?.startsWith("/join") == true) {
                                val token = data.lastPathSegment?.takeIf { it != "join" }
                                navigator.navigate(JoinList(token = token))
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (isAuthenticated) ShoppingList else Welcome
                    ) {
                        featureInitializers.forEach {
                            it.registerGraph(this, navigator, windowSizeClass)
                        }
                    }
                }
            }
        }
    }
}
