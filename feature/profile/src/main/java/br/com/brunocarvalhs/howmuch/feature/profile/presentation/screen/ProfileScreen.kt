package br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.feature.profile.R
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import coil.compose.AsyncImage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    intent: ProfileIntent
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            UserInfoHeader(
                name = state.user?.displayName ?: stringResource(R.string.profile_default_name),
                email = state.user?.email ?: stringResource(R.string.profile_default_email),
                photoUrl = state.user?.photoUrl
            )

            Spacer(modifier = Modifier.height(32.dp))

            ProfileMenuGroup {
                ProfileMenuOption(
                    title = stringResource(R.string.profile_menu_notification_preferences),
                    icon = Icons.Default.Notifications,
                    onClick = { intent.onNavigate(Unit) }
                )
                ProfileMenuDivider()
                ProfileMenuOption(
                    title = stringResource(R.string.profile_menu_theme_currency_language),
                    icon = Icons.Default.Language,
                    onClick = { intent.onNavigate(Unit) }
                )
                ProfileMenuDivider()
                ProfileMenuOption(
                    title = stringResource(R.string.profile_menu_about),
                    icon = Icons.Default.Info,
                    onClick = { intent.onNavigate(Unit) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProfileMenuGroup {
                ProfileMenuOption(
                    title = stringResource(R.string.profile_menu_sign_out),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = { intent.onSignOut() },
                    contentColor = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UserInfoHeader(name: String, email: String, photoUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileAvatar(name = name, photoUrl = photoUrl)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val AVATAR_SIZE_DP = 88

@Composable
private fun ProfileAvatar(name: String, photoUrl: String?) {
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE_DP.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProfileMenuGroup(content: @Composable () -> Unit) {
    CestouCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ProfileMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun ProfileMenuOption(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val previewUser = AuthenticatedUser(
    id = "1",
    email = "isabella@example.com",
    displayName = "Isabella Carvalho"
)

@Preview(showBackground = true, name = "Padrão")
@Composable
private fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileUiState(user = previewUser),
            intent = ProfileIntent()
        )
    }
}

@Preview(showBackground = true, name = "Sem usuário logado")
@Composable
private fun ProfileScreenNoUserPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileUiState(),
            intent = ProfileIntent()
        )
    }
}
