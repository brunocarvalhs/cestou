package br.com.brunocarvalhs.howmuch.core.ui.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Expõe os `testTag` da subárvore como resource-id de acessibilidade, que é o que o Maestro lê
 * sem depender do idioma do aparelho. Sheets, dialogs e menus são janelas próprias: a flag ligada
 * na raiz do app não chega até eles, então cada um precisa aplicá-la de novo.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }
