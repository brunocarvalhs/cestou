package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.navigation.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.Partner
import br.com.brunocarvalhs.howmuch.core.navigation.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.ui.R
import coil.compose.AsyncImage

sealed class BottomNavItem(
    val route: Any,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Lists : BottomNavItem(
        ShoppingList,
        R.string.nav_lists,
        Icons.AutoMirrored.Filled.FormatListBulleted,
        Icons.AutoMirrored.Outlined.FormatListBulleted
    )

    object PartnerItem : BottomNavItem(
        Partner,
        R.string.nav_partner,
        Icons.Default.Group,
        Icons.Outlined.Group
    )

    object AiChatItem : BottomNavItem(
        AiChat,
        R.string.nav_ai_chat,
        Icons.Default.AutoAwesome,
        Icons.Outlined.AutoAwesome
    )

    object ProfileItem : BottomNavItem(
        Profile,
        R.string.nav_profile,
        Icons.Default.AccountCircle,
        Icons.Outlined.AccountCircle
    )
}

@Composable
fun CestouBottomNavigation(
    currentRoute: Any?,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
    photoUrl: String? = null
) {
    val items = listOf(
        BottomNavItem.Lists,
        BottomNavItem.PartnerItem,
        BottomNavItem.AiChatItem,
        BottomNavItem.ProfileItem
    )

    NavigationBar(
        modifier = modifier.height(112.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute != null && currentRoute::class == item.route::class
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (item is BottomNavItem.ProfileItem && photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = stringResource(item.titleRes),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.titleRes)
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                alwaysShowLabel = false
            )
        }
    }
}
