package br.com.brunocarvalhs.howmuch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsParam
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.firstOpen
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackNavigation
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.isFirstAppOpen
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.setStatusBarIconColor
import br.com.brunocarvalhs.howmuch.app.foundation.theme.HowMuchTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        statusBarColor()
        trackLifecycleEvent("onCreate")
        setContent {
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navController = rememberNavController(bottomSheetNavigator)
            navController.trackNavigation()
            HowMuchTheme {
                Surface(
                    modifier = Modifier
                        .imePadding()
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainApp(
                        navController = navController,
                        bottomSheetNavigator = bottomSheetNavigator
                    )
                }
            }
        }

        lifecycleScope.launch {
            if (isFirstAppOpen()) {
                firstOpen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        trackLifecycleEvent("onStart")
    }

    override fun onResume() {
        super.onResume()
        trackLifecycleEvent("onResume")
    }

    override fun onPause() {
        super.onPause()
        trackLifecycleEvent("onPause")
    }

    override fun onStop() {
        super.onStop()
        trackLifecycleEvent("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        trackLifecycleEvent("onDestroy")
    }

    private fun trackLifecycleEvent(eventName: String) {
        AnalyticsEvents.trackEvent(
            event = AnalyticsEvent.LIFECYCLE,
            params = mapOf(
                AnalyticsParam.LIFECYCLE to eventName,
                AnalyticsParam.SCREEN_NAME to "MainActivity",
                AnalyticsParam.TIMESTAMP to System.currentTimeMillis()
            )
        )
    }

    private fun statusBarColor() {
        window?.setStatusBarIconColor()
    }
}
