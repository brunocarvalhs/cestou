package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.WelcomeUiState

@Composable
internal fun WelcomeScreen(
    state: WelcomeUiState,
    intent: WelcomeIntent = WelcomeIntent(),
    actions: @Composable RowScope.() -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            val brandName = stringResource(CoreUiR.string.app_name)

            // Identidade da marca.
            //
            // NAO HA AINDA um asset de logo final da Cestou. O ícone de framework
            // (android.R.drawable.ic_menu_gallery) que existia aqui foi removido por não
            // poder ir para produção/beta. Em seu lugar usamos um lettermark tipográfico
            // (badge "C" + wordmark "Cestou") como identidade visual temporária e
            // defensável — não é um logo definitivo, é um placeholder de marca honesto.
            // TODO(product-owner): substituir por um logo/ícone de marca real assim que
            // existir um asset final (SVG/vetor) fornecido pelo dono do produto.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // O badge e o texto formam uma única unidade de marca; evita que leitores
                // de tela anunciem a inicial e o nome completo separadamente.
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = brandName
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brandName.take(1),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = brandName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Text Content
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                actions()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rodapé com a versão do app — o campo `state.version` e a string
            // `welcome_footer` já existiam mas nunca eram exibidos na tela.
            Text(
                text = stringResource(R.string.welcome_footer, state.version),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen(state = WelcomeUiState(version = "1.2.0")) {
            CestouButton(
                text = "Começar",
                onClick = { },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )
        }
    }
}

@Preview(showBackground = true, name = "Estado padrão pt-BR (versão custom)")
@Composable
private fun WelcomeScreenVersionPreview() {
    MaterialTheme {
        WelcomeScreen(state = WelcomeUiState(version = "2.0.0-beta")) {
            CestouButton(
                text = "Começar",
                onClick = { },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )
        }
    }
}
