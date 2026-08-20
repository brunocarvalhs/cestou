package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.PartnerInfo
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ProfileUiState

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    intent: ProfileIntent
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // User Info Header
            UserInfoHeader(
                name = state.user?.displayName ?: "Usuário",
                email = state.user?.email ?: "email@exemplo.com"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Partner Card
            state.partner?.let { partner ->
                PartnerCard(
                    partner = partner,
                    onDisconnect = { intent.onDisconnectPartner() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Options
            ProfileMenuOption(
                title = "Preferências de Notificação",
                icon = Icons.Default.Notifications,
                onClick = { /* TODO */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileMenuOption(
                title = "Tema, Moeda e Idioma",
                icon = Icons.Default.Language,
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuOption(
                title = "Sobre o App",
                icon = Icons.Default.Info,
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuOption(
                title = "Sair",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = { intent.onSignOut() },
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun UserInfoHeader(name: String, email: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery), // Mock
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PartnerCard(partner: PartnerInfo, onDisconnect: () -> Unit) {
    CestouCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_myplaces), // Mock
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Parceiro Vinculado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = partner.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            TextButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Text(text = "Desconectar")
            }
        }
    }
}

@Composable
private fun ProfileMenuOption(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    CestouCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
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
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    ProfileScreen(
        state = ProfileUiState(
            partner = PartnerInfo("João Souza")
        ),
        intent = ProfileIntent()
    )
}
