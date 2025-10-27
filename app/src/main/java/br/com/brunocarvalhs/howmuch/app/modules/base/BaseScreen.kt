package br.com.brunocarvalhs.howmuch.app.modules.base

import android.app.Activity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.NavBarItem

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun BaseScreen(
    tabs: LinkedHashMap<NavBarItem, @Composable (PaddingValues) -> Unit>
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val tabsNavController = rememberNavController()
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedTab = tabs.keys.find { it.route == currentRoute } ?: tabs.keys.first()

    if (isExpanded) {
        Row {
            AppNavigationRail(
                tabs = tabs,
                selectedTab = selectedTab,
                navController = tabsNavController,
                modifier = Modifier.fillMaxHeight()
            )
            AppContent(
                navController = tabsNavController,
                tabs = tabs,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                AppNavigationBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    navController = tabsNavController,
                    modifier = Modifier.fillMaxWidth()
                )
            },
        ) { padding ->
            AppContent(
                navController = tabsNavController,
                tabs = tabs,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
            )
        }
    }
}

@Composable
private fun AppContent(
    navController: NavHostController,
    tabs: LinkedHashMap<NavBarItem, @Composable (PaddingValues) -> Unit>,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = tabs.entries.first().key.route,
        modifier = modifier
    ) {
        tabs.forEach { (item, content) ->
            composable(item.route) {
                content(PaddingValues())
            }
        }
    }
}

@Composable
private fun AppNavigationBar(
    tabs: LinkedHashMap<NavBarItem, @Composable (PaddingValues) -> Unit>,
    selectedTab: NavBarItem,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        tabs.keys.forEach { item ->
            NavigationBarItem(
                selected = item == selectedTab,
                onClick = { navigateToTab(navController, item.route) },
                label = {
                    Text(
                        text = stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = stringResource(item.label),
                    )
                }
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    tabs: LinkedHashMap<NavBarItem, @Composable (PaddingValues) -> Unit>,
    selectedTab: NavBarItem,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
    ) {
        tabs.keys.forEach { item ->
            NavigationRailItem(
                selected = item == selectedTab,
                onClick = { navigateToTab(navController, item.route) },
                label = {
                    Text(
                        text = stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = stringResource(item.label),
                    )
                }
            )
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
@DevicesPreview
private fun BaseScreenPreview() {
    BaseScreen(
        tabs = linkedMapOf(
            NavBarItem.HOME to { },
            NavBarItem.HISTORY to { },
            NavBarItem.MENU to { }
        )
    )
}
