package br.com.brunocarvalhs.howmuch

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import br.com.brunocarvalhs.howmuch.analytics.ScreenTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.TransitionExtensions
import br.com.brunocarvalhs.howmuch.core.navigation.isProtectedRoute
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.FeedbackSurvey
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.History
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.SpendingInsights
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.StoreReview
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBottomNavigation
import br.com.brunocarvalhs.howmuch.core.ui.utils.exposeTestTags
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.CompleteName
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.isAuthFlow
import br.com.brunocarvalhs.howmuch.notifications.NotificationPermissionRequester
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var featureNavGraphs: Set<@JvmSuppressWildcards FeatureNavGraph>

    @Inject
    lateinit var navigator: Navigator

    @Inject
    internal lateinit var screenTracker: ScreenTracker

    @Inject
    internal lateinit var featureFlagService: FeatureFlagService

    private val fluidTransitions: Boolean
        get() = featureFlagService.isEnabled(FeatureFlagKeys.FLUID_TRANSITIONS_ENABLED, default = true)

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLinkIntent()
        setContent {
            /** Expõe os testTags como resource-id para o Maestro, que não depende do idioma do aparelho. */
            Box(modifier = Modifier.fillMaxSize().exposeTestTags()) {
                CestouApp(windowSizeClass = calculateWindowSizeClass(this@MainActivity))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForeground()
    }

    /** Rotação e troca de tema recriam a Activity sem o app ir para segundo plano. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.onBackground()
    }

    /**
     * With `launchMode="singleTask"` (AndroidManifest.xml), an invite link opened while the app
     * is already running reuses this Activity instead of creating a second instance - which used
     * to leave the resumed, original instance's [Navigator] unbound once the second instance's
     * composition disposed (crash fixed by this change, see Navigator.navigate/goBack). This is
     * where that reused instance receives the new intent.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLinkIntent()
    }

    /**
     * Reads a pending invite token out of [getIntent] into the ViewModel, then replaces this
     * Activity's intent with a blank one. Without this, [getIntent] keeps returning the SAME
     * invite intent across every future recreation (rotation, dark mode, locale change - the
     * Activity isn't destroyed by the OS for those, just recomposed), so `onCreate` would read it
     * again on every recreation and resurrect an already-consumed token (B-14).
     */
    private fun consumeDeepLinkIntent() {
        viewModel.onDeepLink(intent)
        setIntent(Intent())
    }

    @OptIn(ExperimentalLayoutApi::class)
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

            // Bind síncrono, direto no corpo da composição: precisa estar concluído ANTES do
            // NavHost (mais abaixo, na mesma composição) compor seus destinos - inclusive os
            // restaurados do back stack no primeiro frame. Um LaunchedEffect aqui rodaria depois
            // desse primeiro frame, deixando `navigator.navController` desvinculado exatamente
            // quando um destino restaurado (ex.: CartGraph) tenta lê-lo, causando o crash fatal
            // "Navigator not bound to a NavController" visto no Crashlytics. bind() é apenas uma
            // atribuição de var, então chamá-lo a cada (re)composição é seguro e barato.
            navigator.bind(navController)

            // Navigator é singleton do Hilt (sobrevive à recriação da Activity). Ao destruir esta
            // composição (rotação, dark mode, troca de idioma, "não manter atividades" etc.) é
            // preciso desvincular o NavController desta Activity - senão o singleton prende a
            // referência antiga (vazamento) e, se uma nova Activity já tiver feito bind antes
            // deste onDispose rodar, unbind() (condicional por identidade) não apaga o bind novo.
            DisposableEffect(navController) {
                onDispose { navigator.unbind(navController) }
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()

            /**
             * Usa a entrada atual do back stack (dialogs inclusos), não a `screenBackStackEntry`
             * abaixo, que ignora dialogs só para o layout da barra inferior.
             */
            LaunchedEffect(navBackStackEntry) {
                navBackStackEntry?.let { screenTracker.onDestinationChanged(it.id, it.destination) }
            }

            var screenBackStackEntry by remember { mutableStateOf(navBackStackEntry) }
            LaunchedEffect(navBackStackEntry) {
                val destination = navBackStackEntry?.destination
                if (destination != null && destination !is DialogNavigator.Destination) {
                    screenBackStackEntry = navBackStackEntry
                }
            }
            val currentDestination = screenBackStackEntry?.destination
            val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

            val initialAuthenticated = remember { isAuthenticated }
            val requiresName by viewModel.requiresName.collectAsStateWithLifecycle()
            var wasAuthenticated by remember { mutableStateOf(isAuthenticated) }
            LaunchedEffect(isAuthenticated) {
                if (isAuthenticated) {
                    wasAuthenticated = true
                } else if (wasAuthenticated) {
                    wasAuthenticated = false
                    navigator.navigate(Welcome) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            // AiChat's shoppingId here is a throwaway placeholder: rootRoutes only matches by
            // route::class (see hasRoute/isProtectedRoute below), never by field value.
            val rootRoutes = remember { listOf(ShoppingList, History, AiChat(shoppingId = ""), Profile) }

            val currentRoute = rootRoutes.find { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            }

            val isOnProtectedRoute = currentDestination?.isProtectedRoute(rootRoutes + SpendingInsights) == true

            LaunchedEffect(isOnProtectedRoute, isAuthenticated) {
                if (!isAuthenticated && isOnProtectedRoute) {
                    navigator.navigate(Welcome) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            /**
             * An invite token read by onDeepLink/onNewIntent is held in the ViewModel (not
             * navigated to immediately) because JoinList isn't a protected route (MobileRoutes.kt)
             * and would otherwise open over Welcome and fail to join while signed out (B-30). Also
             * waits for `!requiresName` so it never races the required-name gate below: both would
             * otherwise navigate on the same isAuthenticated flip, and whichever runs last wins -
             * dropping the invite silently since the token is already consumed by then.
             */
            val pendingJoinToken by viewModel.pendingJoinToken.collectAsStateWithLifecycle()
            LaunchedEffect(pendingJoinToken, isAuthenticated, requiresName) {
                val token = pendingJoinToken
                if (token != null && isAuthenticated && !requiresName) {
                    navigator.navigate(JoinList(token = token))
                    viewModel.consumePendingJoinToken()
                }
            }

            /**
             * Pareamento pedido pelo relógio (sdd-055). Espera o login e o nome, como o convite: sem
             * login, a ViewModel já respondeu NOT_SIGNED_IN ao relógio.
             */
            val pendingWearPairing by viewModel.pendingWearPairing.request.collectAsStateWithLifecycle()
            LaunchedEffect(pendingWearPairing, isAuthenticated, requiresName) {
                val request = pendingWearPairing
                if (request != null && isAuthenticated && !requiresName) {
                    navigator.navigate(request) { launchSingleTop = true }
                    viewModel.pendingWearPairing.consume()
                }
            }

            /**
             * Toque numa notificação da IA: abre a conversa. Espera o login e o nome (como o
             * convite acima) para não navegar por cima da tela de boas-vindas.
             */
            val pendingAiChat by viewModel.pendingAiChatSession.collectAsStateWithLifecycle()
            LaunchedEffect(pendingAiChat, isAuthenticated, requiresName) {
                val sessionId = pendingAiChat
                if (sessionId != null && isAuthenticated && !requiresName) {
                    /** Se um chat já está aberto (app voltou de segundo plano nele), troca em vez de empilhar outro. */
                    navigator.navigate(AiChat(sessionId = sessionId)) {
                        popUpTo<AiChat> { inclusive = true }
                        launchSingleTop = true
                    }
                    viewModel.consumePendingAiChatSession()
                }
            }

            /**
             * Pede a permissão de notificações uma vez por instalação, depois do login: sem ela os
             * lembretes e os avisos da IA não aparecem. Recusar não bloqueia nada.
             */
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(isAuthenticated, requiresName) {
                val requester = NotificationPermissionRequester(applicationContext)
                if (isAuthenticated && !requiresName && requester.shouldAsk()) {
                    requester.markAsked()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Required-name gate (spec EPA-06): a signed-in account without a name never reaches the
            // app. Skipped inside the auth flow, where a just-created account has no name for a
            // moment; the decision re-reads the auth state so a name saved a moment ago is honored.
            LaunchedEffect(requiresName, currentDestination) {
                val destination = currentDestination ?: return@LaunchedEffect
                if (requiresName && !destination.isAuthFlow() && viewModel.requiresNameNow()) {
                    navigator.navigate(CompleteName) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            /**
             * Pesquisa de satisfação (sdd-034): só abre num destino estável (uma das abas, sem dialog
             * por cima) e com o teclado fechado. Fluxos de auth, carrinho, formulários, paywall e
             * scanner não são abas, então nunca são interrompidos. O NPS é pedido ao chegar na lista.
             */
            val pendingFeedback by viewModel.pendingFeedback.collectAsStateWithLifecycle()
            val onDialog = navBackStackEntry?.destination is DialogNavigator.Destination
            val imeVisible = WindowInsets.isImeVisible
            val onStableTab = isAuthenticated && !requiresName && !onDialog && !imeVisible &&
                currentRoute != null && currentRoute !is AiChat
            LaunchedEffect(currentRoute, onStableTab) {
                if (onStableTab && currentRoute == ShoppingList) viewModel.onShoppingListReached()
            }
            LaunchedEffect(pendingFeedback, onStableTab) {
                val prompt = pendingFeedback
                if (prompt != null && onStableTab) {
                    viewModel.onFeedbackPromptShown(prompt)
                    if (prompt.kind == SurveyKind.STORE_REVIEW) {
                        navigator.navigate(StoreReview)
                    } else {
                        navigator.navigate(FeedbackSurvey(kind = prompt.kind.name, feature = prompt.feature))
                    }
                }
            }

            Scaffold(
                bottomBar = {
                    CestouBottomNavigation(
                        currentRoute = currentRoute,
                        photoUrl = photoUrl,
                        visible = currentRoute != null && currentRoute !is AiChat,
                        onNavigate = { route ->
                            screenTracker.onTabSelected(route)
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
                    val startDestination = remember {
                        when {
                            !initialAuthenticated -> Welcome
                            viewModel.requiresNameNow() -> CompleteName
                            else -> ShoppingList
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = { TransitionExtensions.forwardEnter(fluidTransitions) },
                        exitTransition = { TransitionExtensions.forwardExit(fluidTransitions) },
                        popEnterTransition = { TransitionExtensions.backEnter(fluidTransitions) },
                        popExitTransition = { TransitionExtensions.backExit(fluidTransitions) }
                    ) {
                        featureNavGraphs.forEach {
                            it.registerGraph(this, navigator, windowSizeClass)
                        }
                    }
                }
            }
        }
    }
}
