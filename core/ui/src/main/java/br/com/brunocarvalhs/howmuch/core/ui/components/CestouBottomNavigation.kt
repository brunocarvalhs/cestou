package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.navigation.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.Partner
import br.com.brunocarvalhs.howmuch.core.navigation.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.ui.R

sealed class BottomNavItem(
    val route: Any,
    val titleRes: Int,
    val icon: ImageVector
) {
    object Lists : BottomNavItem(ShoppingList, R.string.nav_lists, Icons.AutoMirrored.Filled.FormatListBulleted)
    object PartnerItem : BottomNavItem(Partner, R.string.nav_partner, Icons.Default.Group)
    object AiChatItem : BottomNavItem(AiChat, R.string.nav_ai_chat, Icons.Default.AutoAwesome)
    object ProfileItem : BottomNavItem(Profile, R.string.nav_profile, Icons.Default.AccountCircle)
}

@Composable
fun CestouBottomNavigation(
    currentRoute: Any?,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Lists,
        BottomNavItem.PartnerItem,
        BottomNavItem.AiChatItem,
        BottomNavItem.ProfileItem
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute != null && currentRoute::class == item.route::class
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.titleRes)
                    )
                },
                label = {
                    Text(text = stringResource(item.titleRes))
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
