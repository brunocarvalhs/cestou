package br.com.brunocarvalhs.howmuch.core.ui.utils

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun `DynamicString asString with context returns its own value ignoring context`() {
        val context = mockk<Context>()

        val result = UiText.DynamicString("hello").asString(context)

        assertEquals("hello", result)
    }

    @Test
    fun `StringResource asString with context resolves through context getString`() {
        val context = mockk<Context>()
        every { context.getString(42, "World") } returns "Hello World"

        val result = UiText.StringResource(42, "World").asString(context)

        assertEquals("Hello World", result)
    }

    @Test
    fun `StringResource asString with context and no args resolves through context getString`() {
        val context = mockk<Context>()
        every { context.getString(7, *emptyArray<Any>()) } returns "Plain"

        val result = UiText.StringResource(7).asString(context)

        assertEquals("Plain", result)
    }
}
