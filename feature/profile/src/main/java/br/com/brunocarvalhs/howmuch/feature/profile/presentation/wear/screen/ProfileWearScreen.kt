package br.com.brunocarvalhs.howmuch.feature.profile.presentation.wear.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.feature.profile.R
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel.ProfileViewModel

/**
 * Configurações do relógio (sdd-056, Q14): o perfil já existente, com "Abrir no celular" e um
 * alerta antes de "Sair". Sair é só do relógio.
 */
@Composable
internal fun ProfileWearScreen(
    viewModel: ProfileViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileWearContent(
        state = state,
        onLogout = { viewModel.intent.onSignOut() },
        onLinkAccount = { viewModel.intent.onLinkMobileDevice() },
        onOpenPhone = { viewModel.intent.onOpenPhone() }
    )
}

@Composable
private fun ProfileWearContent(
    state: ProfileUiState,
    onLogout: () -> Unit,
    onLinkAccount: () -> Unit,
    onOpenPhone: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.profile_wear_settings_title))
                }
            }

            item {
                Text(
                    text = state.user?.displayName ?: "Usuário",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = onOpenPhone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.profile_wear_open_phone))
                }
            }

            item {
                Button(
                    onClick = onLinkAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.profile_wear_link_phone))
                }
            }

            item {
                OutlinedButton(
                    onClick = { confirmSignOut = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.profile_menu_sign_out))
                }
            }
        }
    }

    AlertDialog(
        visible = confirmSignOut,
        onDismissRequest = { confirmSignOut = false },
        confirmButton = {
            Button(onClick = {
                confirmSignOut = false
                onLogout()
            }) {
                Text(stringResource(R.string.profile_menu_sign_out))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { confirmSignOut = false }) {
                Text(stringResource(R.string.profile_wear_cancel))
            }
        },
        title = { Text(stringResource(R.string.profile_wear_sign_out_title)) },
        text = { Text(stringResource(R.string.profile_wear_sign_out_message)) }
    )
}

@WearPreviewDevices
@Composable
private fun ProfileWearContentPreview() {
    MaterialTheme {
        ProfileWearContent(
            state = ProfileUiState(
                user = AuthenticatedUser(
                    id = "1",
                    email = "bruno@example.com",
                    displayName = "Bruno Carvalho"
                )
            ),
            onLogout = {},
            onLinkAccount = {},
            onOpenPhone = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ProfileWearContentNoUserPreview() {
    MaterialTheme {
        ProfileWearContent(
            state = ProfileUiState(),
            onLogout = {},
            onLinkAccount = {},
            onOpenPhone = {}
        )
    }
}
