package br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.UserAvatar
import br.com.brunocarvalhs.howmuch.feature.profile.R
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.components.ProfileSettingsSection
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    intent: ProfileIntent,
    settingsState: SettingsUiState = SettingsUiState(),
    settingsIntent: SettingsIntent = SettingsIntent(),
    subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                id = state.user?.id.orEmpty(),
                name = state.user?.displayName,
                email = state.user?.email,
                photoUrl = state.user?.photoUrl
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(24.dp))

                SubscriptionStatusCard(
                    status = subscriptionStatus,
                    onManageSubscription = intent.onManageSubscription
                )

                Spacer(modifier = Modifier.height(24.dp))

                ProfileFlatRow(
                    title = stringResource(R.string.profile_link_wear_device),
                    icon = Icons.Default.Watch,
                    onClick = {
                        if (subscriptionStatus == SubscriptionStatus.PRO) {
                            intent.onLinkWearDevice()
                        } else {
                            intent.onManageSubscription()
                        }
                    }
                )
                HorizontalDivider()

                ProfileSettingsSection(
                    settingsState = settingsState,
                    settingsIntent = settingsIntent,
                    modifier = Modifier.fillMaxWidth()
                )

                ProfileFlatRow(
                    title = stringResource(R.string.profile_menu_sign_out),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = { intent.onSignOut() },
                    contentColor = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Full-bleed color block behind the status bar, avatar + name/email laid out side by side
 * (iFood-style Profile header) instead of a separate "Perfil" top app bar.
 */
@Composable
private fun ProfileHeader(id: String, name: String?, email: String?, photoUrl: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        UserAvatar(
            profile = UserProfile(id = id, name = name, photoUrl = photoUrl),
            size = AVATAR_SIZE_DP.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            email?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val AVATAR_SIZE_DP = 64

/** Flat row (no card, no background), used for the items that sit outside the settings accordion. */
@Composable
private fun ProfileFlatRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        },
        headlineContent = {
            Text(text = title, color = contentColor)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/** Flat, tinted promo banner (no elevation/shadow) - matches the iFood-style promo rows. */
@Composable
private fun SubscriptionStatusCard(
    status: SubscriptionStatus,
    onManageSubscription: () -> Unit
) {
    val isPro = status == SubscriptionStatus.PRO
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onManageSubscription),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isPro) R.string.profile_subscription_pro_title else R.string.profile_subscription_free_title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        if (isPro) {
                            R.string.profile_subscription_pro_subtitle
                        } else {
                            R.string.profile_subscription_free_subtitle
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

private val previewUser = AuthenticatedUser(
    id = "1",
    email = "isabella@example.com",
    displayName = "Isabella Carvalho"
)

@PreviewCestouScreens
@Composable
private fun ProfileScreenPreview() {
    CestouTheme {
        ProfileScreen(
            state = ProfileUiState(user = previewUser),
            intent = ProfileIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun ProfileScreenProPreview() {
    CestouTheme {
        ProfileScreen(
            state = ProfileUiState(user = previewUser),
            intent = ProfileIntent(),
            subscriptionStatus = SubscriptionStatus.PRO
        )
    }
}

@PreviewCestouScreens
@Composable
private fun ProfileScreenNoUserPreview() {
    CestouTheme {
        ProfileScreen(
            state = ProfileUiState(),
            intent = ProfileIntent()
        )
    }
}
