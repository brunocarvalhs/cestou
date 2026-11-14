package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearProViewModel
import kotlinx.coroutines.delay

/** Tags da tela de assinatura, para os testes de UI (AGENTS §5: sem asserts de texto). */
internal const val TAG_PRO_REQUIRED = "wear_pro_required"
internal const val TAG_PRO_OPEN_PHONE = "wear_pro_open_phone"
internal const val TAG_PRO_LOADING = "wear_pro_loading"

/**
 * Prazo para o celular mandar o status PRO. Sem resposta até lá, o relógio decide como FREE e
 * mostra a tela de assinatura, em vez de ficar carregando para sempre.
 */
internal const val PRO_STATUS_TIMEOUT_MS = 5_000L

/**
 * Mostra [content] só com assinatura PRO. Enquanto o status não chega do celular, mostra um
 * carregamento, sem abrir a tela de assinatura. Conta grátis, ou prazo esgotado sem resposta,
 * mostram a tela de assinatura. Vale para qualquer tela do relógio.
 */
@Composable
internal fun WearProGate(viewModel: WearProViewModel, content: @Composable () -> Unit) {
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    var timedOut by remember { mutableStateOf(false) }

    LaunchedEffect(isPro == null) {
        if (isPro == null) {
            delay(PRO_STATUS_TIMEOUT_MS)
            timedOut = true
        }
    }

    when {
        isPro == true -> content()
        isPro == false || timedOut -> WearProRequiredScreen(onOpenPhone = { viewModel.openPhone() })
        else -> WearProLoadingScreen()
    }
}

@Composable
private fun WearProLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(TAG_PRO_LOADING))
    }
}

@Composable
private fun WearProRequiredScreen(onOpenPhone: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_pro_title))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.shopping_wear_pro_required),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .testTag(TAG_PRO_REQUIRED)
                )
            }

            proButton(onOpenPhone = onOpenPhone, spec = spec)
        }
    }
}

private fun TransformingLazyColumnScope.proButton(onOpenPhone: () -> Unit, spec: TransformationSpec) {
    item {
        Button(
            onClick = onOpenPhone,
            modifier = Modifier
                .fillMaxWidth()
                .transformedHeight(this, spec)
                .testTag(TAG_PRO_OPEN_PHONE),
            colors = wearCardColors(),
            transformation = SurfaceTransformation(spec)
        ) {
            Text(stringResource(R.string.shopping_wear_pro_open))
        }
    }
}
