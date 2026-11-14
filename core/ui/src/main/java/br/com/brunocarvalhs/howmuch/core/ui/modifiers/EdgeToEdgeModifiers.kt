package br.com.brunocarvalhs.howmuch.core.ui.modifiers

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

/** Cabeçalho no topo da tela: só a barra de status; a barra de navegação fica por conta do conteúdo. */
fun Modifier.topBarEdgePadding(): Modifier = statusBarsPadding()

/** Tela inteira sem Scaffold: barras de status e de navegação. */
fun Modifier.screenEdgePadding(): Modifier = statusBarsPadding().navigationBarsPadding()

/** Tela com campo de texto: [screenEdgePadding] mais o teclado. */
fun Modifier.edgeToEdgePadding(): Modifier = screenEdgePadding().imePadding()
