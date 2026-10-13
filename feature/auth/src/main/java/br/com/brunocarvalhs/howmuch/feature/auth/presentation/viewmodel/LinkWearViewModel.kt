package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui.GoogleIdTokenProvider
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.LinkWearIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.LinkWearUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LINK_CODE_LENGTH = 6

@HiltViewModel
internal class LinkWearViewModel @Inject constructor(
    private val wearableSyncService: WearableSyncService,
    private val authService: AuthService,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val featureFlagService: FeatureFlagService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkWearUiState())
    val uiState = _uiState.asStateFlow()

    val intent = LinkWearIntent(
        onCodeChange = { code ->
            _uiState.update { it.copy(code = code, error = null) }
        },
        onLinkClick = { context -> linkDevice(context) }
    )

    private fun linkDevice(context: Context) {
        val code = _uiState.value.code
        if (!featureFlagService.isEnabled(FeatureFlagKeys.WEAR_LOGIN_ENABLED, default = true)) {
            _uiState.update { it.copy(error = "Relógio indisponível no momento") }
            return
        }
        if (code.length != LINK_CODE_LENGTH) {
            _uiState.update { it.copy(error = "Código inválido (6 dígitos)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val error = link(context, code)
            _uiState.update { it.copy(isLoading = false).withLinkResult(error) }
        }
    }

    // The watch signs in to Firebase itself with this Google ID token: the rules need a real
    // session there, a copied uid isn't one (AD-009). E-mail accounts have no token to hand over.
    private suspend fun link(context: Context, code: String): String? {
        val nodeId = wearableSyncService.findNodeByPairingCode(code) ?: return "Dispositivo não encontrado"
        val user = authService.currentUser
        val credential = googleIdTokenProvider.get(context)
        return when {
            user == null || credential == null -> "O relógio só funciona com conta Google"
            !credential.id.equals(user.email, ignoreCase = true) ->
                "Escolha a mesma conta Google que você usa no app"
            else -> {
                wearableSyncService.sendAuthPairToNode(nodeId, user.id, credential.idToken)
                null
            }
        }
    }

    private fun LinkWearUiState.withLinkResult(error: String?) =
        if (error == null) copy(isSuccess = true) else copy(error = error)
}
