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
import br.com.brunocarvalhs.howmuch.core.navigation.isProtectedRoute
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBottomNavigation
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
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
        // Confirming/ruling out whether the Activity itself is being recreated (e.g. by a
        // locale/config change) mid session, which would explain @remember-scoped state
        // (wasAuthenticated, initialAuthenticated) silently resetting to its cold-start default.
        Timber.tag(NAV_DEBUG_TAG).d(
            "onCreate: savedInstanceState=%s taskId=%s",
            savedInstanceState != null,
            taskId
        )
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CestouApp(windowSizeClass = calculateWindowSizeClass(this))
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        Timber.tag(NAV_DEBUG_TAG).d("onConfigurationChanged: locales=%s", newConfig.locales)
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        Timber.tag(NAV_DEBUG_TAG).d("onDestroy: isFinishing=%s isChangingConfigurations=%s", isFinishing, isChangingConfigurations)
        super.onDestroy()
    }

    @Composable
    private fun CestouApp(windowSizeClass: WindowSizeClass) {
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val language by viewModel.language.collectAsStateWithLifecycle()
        val photoUrl by viewModel.photoUrl.collectAsStateWithLifecycle()

        LaunchedEffect(language) {
            Timber.tag(NAV_DEBUG_TAG).d(
                "language effect fired: language=%s currentAppLocales=%s",
                language,
                AppCompatDelegate.getApplicationLocales()
            )
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
                Timber.tag(NAV_DEBUG_TAG).d(
                    "backStackEntry changed -> %s | full stack: %s",
                    destination?.route,
                    navController.currentBackStack.value.joinToString { it.destination.route.toString() }
                )
                if (destination != null && destination !is DialogNavigator.Destination) {
                    screenBackStackEntry = navBackStackEntry
                }
            }
            val currentDestination = screenBackStackEntry?.destination
            val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

            // Captured once: NavHost's startDestination below must never react to later
            // isAuthenticated changes. Navigation-Compose rebuilds the whole graph and resets
            // the back stack whenever startDestination changes on an already-created
            // NavController, which fights with the explicit navigate()+popUpTo() below (and with
            // sign-in's own navigate() in AuthInitializerImpl) over who controls the stack — the
            // exact kind of conflict that can leave a stale, already-authenticated destination on
            // top after sign-out. All auth-driven navigation after the first frame must go
            // through the imperative navigate() calls only.
            val initialAuthenticated = remember { isAuthenticated }
            var wasAuthenticated by remember { mutableStateOf(isAuthenticated) }
            LaunchedEffect(isAuthenticated) {
                Timber.tag(NAV_DEBUG_TAG).d(
                    "isAuthenticated effect fired: isAuthenticated=%s wasAuthenticated=%s stack=%s",
                    isAuthenticated,
                    wasAuthenticated,
                    navController.currentBackStack.value.joinToString { it.destination.route.toString() }
                )
                if (isAuthenticated) {
                    wasAuthenticated = true
                } else if (wasAuthenticated) {
                    wasAuthenticated = false
                    Timber.tag(NAV_DEBUG_TAG).d("navigating to Welcome and clearing back stack")
                    navigator.navigate(Welcome) {
                        // popUpTo(0) is the legacy int-route overload and never matches anything
                        // in this type-safe graph, so it silently popped nothing: every sign-out
                        // just pushed a new Welcome on top of the still-live ShoppingList/etc.
                        // back stack instead of clearing it (compare the correct
                        // popUpTo(Welcome) used on sign-in in AuthInitializerImpl). Popping up to
                        // the graph's own root id clears the entire stack regardless of route type.
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    Timber.tag(NAV_DEBUG_TAG).d(
                        "post-navigate stack=%s",
                        navController.currentBackStack.value.joinToString { it.destination.route.toString() }
                    )
                }
            }

            val rootRoutes = remember { listOf(ShoppingList, AiChat, Profile) }

            val currentRoute = rootRoutes.find { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            }

            // Whether the current destination requires auth is a property of the route itself
            // (RouteProtocol.routeType), declared where each route is defined — not a hardcoded
            // list here that has to be kept in sync by hand as routes are added elsewhere.
            val isOnProtectedRoute = currentDestination?.isProtectedRoute(rootRoutes) == true

            // Continuous guard, separate from the isAuthenticated transition effect above: that
            // effect only fires on the true->false EDGE, so if the user ever lands back on a
            // protected destination while already signed out — e.g. a still-tappable bottom bar
            // surviving one extra frame after sign-out — isAuthenticated never changes again and
            // nothing redirects them. Logcat confirmed this exact case: isAuthenticated stayed
            // false for 10+ real seconds while ShoppingList/Profile were fully navigable. This
            // re-checks on every destination change, independent of *how* the user got there.
            LaunchedEffect(isOnProtectedRoute, isAuthenticated) {
                if (!isAuthenticated && isOnProtectedRoute) {
                    Timber.tag(NAV_DEBUG_TAG).d(
                        "guard: unauthenticated on protected route %s, redirecting to Welcome",
                        currentDestination?.route
                    )
                    navigator.navigate(Welcome) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            val showBottomBar = currentRoute != null

            Scaffold(
                bottomBar = {
                    CestouBottomNavigation(
                        currentRoute = currentRoute,
                        photoUrl = photoUrl,
                        visible = showBottomBar,
                        onNavigate = { route ->
                            Timber.tag(NAV_DEBUG_TAG).d("bottom nav tapped: route=%s", route)
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
                        startDestination = if (initialAuthenticated) ShoppingList else Welcome
                    ) {
                        featureInitializers.forEach {
                            it.registerGraph(this, navigator, windowSizeClass)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val NAV_DEBUG_TAG = "AuthNavDebug"
    }
}
