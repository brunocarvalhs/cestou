package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * `ModalBottomSheet` do app com o fundo (e, portanto, o do drag handle) em
 * `colorScheme.background`.
 *
 * O container padrão do Material 3 é `surfaceContainerLow`, que este tema não define e cai num
 * roxo-acinzentado do baseline; telas com `Scaffold` interno pintam `background` por cima e o
 * handle ficava numa cor diferente do corpo. Os demais parâmetros espelham os do componente
 * original.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CestouModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = MaterialTheme.colorScheme.background,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetGesturesEnabled: Boolean = true,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        dragHandle = dragHandle,
        sheetGesturesEnabled = sheetGesturesEnabled,
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}
