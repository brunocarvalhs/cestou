package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext

@Composable
internal fun CustomAuthenticatedContent(
    state: AuthState,
    uiContext: AuthSuccessUiContext
) {
    when (state) {
        is AuthState.Success -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bem-vindo!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Você acessou como ${state.user.email ?: state.user.uid}",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                CestouButton(text = "Gerenciar MFA", onClick = uiContext.onManageMfa)
                Spacer(modifier = Modifier.height(16.dp))
                CestouButton(
                    text = "Sair",
                    onClick = uiContext.onSignOut,
                    containerColor = MaterialTheme.colorScheme.error
                )
            }
        }

        is AuthState.RequiresEmailVerification -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verifique seu E-mail",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enviamos um link de confirmação para ${state.email}. Por favor, verifique sua caixa de entrada.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                CestouButton(text = "Já verifiquei", onClick = uiContext.onReloadUser)
                Spacer(modifier = Modifier.height(16.dp))
                CestouButton(text = "Reenviar e-mail", onClick = { state.user.sendEmailVerification() })
            }
        }

        is AuthState.RequiresProfileCompletion -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Complete seu Perfil",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Faltam os campos: ${state.missingFields.joinToString()}")
            }
        }

        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
