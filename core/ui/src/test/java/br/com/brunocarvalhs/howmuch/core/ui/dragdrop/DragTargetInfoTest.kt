package br.com.brunocarvalhs.howmuch.core.ui.dragdrop

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

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
        info.dragPosition = Offset(10f, 20f)
        info.dragOffset = Offset(1f, 2f)
        info.dataToDrop = payload

        assertEquals(true, info.isDragging)
        assertEquals(Offset(10f, 20f), info.dragPosition)
        assertEquals(Offset(1f, 2f), info.dragOffset)
        assertEquals(payload, info.dataToDrop)
    }
}
