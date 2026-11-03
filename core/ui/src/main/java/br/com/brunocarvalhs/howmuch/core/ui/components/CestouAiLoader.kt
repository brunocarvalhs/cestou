package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

private val DEFAULT_LOADER_SIZE = 36.dp

/**
 * Loading padrão das telas em que a IA trabalha (chat e análise de imagem): a mesma animação em
 * todo lugar, em vez de um indicador circular por tela. Reserva o espaço enquanto a animação
 * carrega, para o layout não pular.
 */
@Composable
fun CestouAiLoader(
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_LOADER_SIZE,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ai_thinking_loader))
    Box(modifier = modifier.size(size).testTag("ai_loader")) {
        composition?.let {
            LottieAnimation(
                composition = it,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(size),
            )
        }
    }
}
