package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Transições padrão entre telas. Ir para uma tela nova entra pela direita e a anterior sai pela
 * esquerda; voltar faz o caminho inverso. Os deslocamentos ficam em [TransitionOffsets], porque o objeto
 * de transição do Compose não expõe o que foi configurado.
 */
object TransitionExtensions {
    private const val DEFAULT_ANIMATION_DURATION_MS = 300
    private const val COMPOSE_DEFAULT_DURATION_MS = 700

    fun slideInFromRight(duration: Int = DEFAULT_ANIMATION_DURATION_MS): EnterTransition =
        slideInHorizontally(tween(duration), TransitionOffsets::enterFromRight) + fadeIn(tween(duration))

    fun slideOutToLeft(duration: Int = DEFAULT_ANIMATION_DURATION_MS): ExitTransition =
        slideOutHorizontally(tween(duration), TransitionOffsets::exitToLeft) + fadeOut(tween(duration))

    fun slideInFromLeft(duration: Int = DEFAULT_ANIMATION_DURATION_MS): EnterTransition =
        slideInHorizontally(tween(duration), TransitionOffsets::enterFromLeft) + fadeIn(tween(duration))

    fun slideOutToRight(duration: Int = DEFAULT_ANIMATION_DURATION_MS): ExitTransition =
        slideOutHorizontally(tween(duration), TransitionOffsets::exitToRight) + fadeOut(tween(duration))

    fun forwardEnter(fluid: Boolean): EnterTransition = if (fluid) slideInFromRight() else defaultEnter()

    fun forwardExit(fluid: Boolean): ExitTransition = if (fluid) slideOutToLeft() else defaultExit()

    fun backEnter(fluid: Boolean): EnterTransition = if (fluid) slideInFromLeft() else defaultEnter()

    fun backExit(fluid: Boolean): ExitTransition = if (fluid) slideOutToRight() else defaultExit()

    /** Comportamento anterior do `NavHost` (só fade), usado quando `feat_fluid_transitions` está desligada. */
    fun defaultEnter(): EnterTransition = fadeIn(tween(COMPOSE_DEFAULT_DURATION_MS))

    fun defaultExit(): ExitTransition = fadeOut(tween(COMPOSE_DEFAULT_DURATION_MS))
}

/** Deslocamentos horizontais de cada transição, à parte para os testes conferirem a direção. */
internal object TransitionOffsets {
    fun enterFromRight(fullWidth: Int): Int = fullWidth

    fun exitToLeft(fullWidth: Int): Int = -fullWidth

    fun enterFromLeft(fullWidth: Int): Int = -fullWidth

    fun exitToRight(fullWidth: Int): Int = fullWidth
}
