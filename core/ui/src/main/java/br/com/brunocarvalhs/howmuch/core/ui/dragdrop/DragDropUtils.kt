package br.com.brunocarvalhs.howmuch.core.ui.dragdrop

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize

private const val DRAG_SCALE = 1.1f
private const val DRAG_ALPHA = 0.8f

val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }

@Stable
class DragTargetInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition: Offset by mutableStateOf(Offset.Zero)
    var dragOffset: Offset by mutableStateOf(Offset.Zero)
    var draggableComposable by mutableStateOf<(@Composable () -> Unit)?>(null)
    var dataToDrop by mutableStateOf<Any?>(null)
}

@Composable
fun DragAndDropContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = remember { DragTargetInfo() }
    CompositionLocalProvider(
        LocalDragTargetInfo provides state
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            if (state.isDragging) {
                var targetSize by remember { mutableStateOf(IntSize.Zero) }
                Box(modifier = Modifier
                    .graphicsLayer {
                        val offset = (state.dragPosition + state.dragOffset)
                        scaleX = DRAG_SCALE
                        scaleY = DRAG_SCALE
                        alpha = if (targetSize == IntSize.Zero) 0f else DRAG_ALPHA
                        translationX = offset.x.minus(targetSize.width / 2)
                        translationY = offset.y.minus(targetSize.height / 2)
                    }
                    .onGloballyPositioned {
                        targetSize = it.size
                    }
                ) {
                    state.draggableComposable?.invoke()
                }
            }
        }
    }
}

@Composable
fun <T> DragTarget(
    modifier: Modifier = Modifier,
    dataToDrop: T,
    content: @Composable (() -> Unit)
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    val currentState = LocalDragTargetInfo.current

    Box(modifier = modifier
        .onGloballyPositioned {
            currentPosition = it.positionInWindow()
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    currentState.dataToDrop = dataToDrop
                    currentState.isDragging = true
                    currentState.dragPosition = currentPosition + offset
                    currentState.draggableComposable = content
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                },
                onDragEnd = {
                    currentState.isDragging = false
                    currentState.dragOffset = Offset.Zero
                },
                onDragCancel = {
                    currentState.isDragging = false
                    currentState.dragOffset = Offset.Zero
                }
            )
        }
    ) {
        content()
    }
}

@Composable
fun DropTarget(
    modifier: Modifier = Modifier,
    onDataDropped: (Any) -> Unit,
    content: @Composable (BoxScope.(isHovered: Boolean, data: Any?) -> Unit)
) {
    val dragInfo = LocalDragTargetInfo.current
    val dragPosition = dragInfo.dragPosition + dragInfo.dragOffset
    var isHovered by remember { mutableStateOf(false) }

    Box(modifier = modifier.onGloballyPositioned {
        val rect = it.boundsInWindow()
        isHovered = dragInfo.isDragging && rect.contains(dragPosition)
    }) {
        content(isHovered, if (isHovered) dragInfo.dataToDrop else null)
        
        // Handle drop
        LaunchedEffect(dragInfo.isDragging) {
            if (!dragInfo.isDragging && isHovered) {
                dragInfo.dataToDrop?.let {
                    onDataDropped(it)
                }
            }
        }
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.boundsInWindow(): androidx.compose.ui.geometry.Rect {
    val position = positionInWindow()
    return androidx.compose.ui.geometry.Rect(
        position.x,
        position.y,
        position.x + size.width,
        position.y + size.height
    )
}
