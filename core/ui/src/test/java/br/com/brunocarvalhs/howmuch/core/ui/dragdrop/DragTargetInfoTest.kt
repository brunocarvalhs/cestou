package br.com.brunocarvalhs.howmuch.core.ui.dragdrop

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

private const val DRAG_X = 10f
private const val DRAG_Y = 20f

class DragTargetInfoTest {

    @Test
    fun `defaults to not dragging with zero offsets and no payload`() {
        val info = DragTargetInfo()

        assertFalse(info.isDragging)
        assertEquals(Offset.Zero, info.dragPosition)
        assertEquals(Offset.Zero, info.dragOffset)
        assertNull(info.draggableComposable)
        assertNull(info.dataToDrop)
    }

    @Test
    fun `tracks drag state as it is mutated`() {
        val info = DragTargetInfo()
        val payload = "product-1"

        info.isDragging = true
        info.dragPosition = Offset(DRAG_X, DRAG_Y)
        info.dragOffset = Offset(1f, 2f)
        info.dataToDrop = payload

        assertEquals(true, info.isDragging)
        assertEquals(Offset(DRAG_X, DRAG_Y), info.dragPosition)
        assertEquals(Offset(1f, 2f), info.dragOffset)
        assertEquals(payload, info.dataToDrop)
    }
}
