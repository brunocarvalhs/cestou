package br.com.brunocarvalhs.howmuch.app.modules.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.brunocarvalhs.howmuch.BuildConfig
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvents.trackEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsParam
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.getAppVersion
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.openPlayStore
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.routes.SubscriptionGraphRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val version = context.getAppVersion()
    val isDebug = BuildConfig.DEBUG

    val services = listOf(
        ServiceItem(R.string.rate_on_google_play) {
            context.openPlayStore()
        },
        ServiceItem(R.string.menu_subscription) {
            navController.navigate(SubscriptionGraphRoute)
        }
    )

    LaunchedEffect(Unit) {
        trackEvent(
            AnalyticsEvent.SCREEN_VIEW,
            mapOf(
                AnalyticsParam.SCREEN_NAME to "MenuScreen",
                AnalyticsParam.APP_VERSION to version,
                AnalyticsParam.DEBUG to isDebug.toString()
            )
        )
    }

    MenuContent(
        services = services,
        version = version,
        isDebug = isDebug
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuContent(
    services: List<ServiceItem>,
    version: String,
    isDebug: Boolean,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_bar_menu)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(services) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            service.onClick()
                            trackClick(
                                viewId = "service_${service.title}",
                                viewName = context.getString(service.title),
                                screenName = "MenuScreen"
                            )
                        },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(service.title),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Versão: $version" + if (isDebug) " (Debug)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@DevicesPreview
private fun MenuContentPreview() {
    val services = listOf(
        ServiceItem(R.string.rate_on_google_play) {},
        ServiceItem(R.string.rate_on_google_play) {},
        ServiceItem(R.string.rate_on_google_play) {},
    )

    MenuContent(
        services = services,
        version = "1.0.0",
        isDebug = true
    )
}
