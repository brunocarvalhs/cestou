package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue

/**
 * Regra do `confirmValueChange` do sheet do chat. Com o chat ativo o sheet fica fixo em
 * `Expanded` (o arrasto já está desligado), mas `Hidden` precisa passar: voltar, toque no scrim e
 * "fechar" pedem `hide()` ao `sheetState` e só então disparam `onDismissRequest`. Vetar o `Hidden`
 * deixava o sheet preso com o scrim na tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun isSheetValueAllowed(isChatActive: Boolean, target: SheetValue): Boolean =
    !isChatActive || target == SheetValue.Expanded || target == SheetValue.Hidden
