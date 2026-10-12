package br.com.brunocarvalhs.howmuch.core.ui.utils

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

private const val TEST_RES_ID_WITH_ARGS = 42
private const val TEST_RES_ID_PLAIN = 7

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
        every { context.getString(TEST_RES_ID_WITH_ARGS, "World") } returns "Hello World"

        val result = UiText.StringResource(TEST_RES_ID_WITH_ARGS, "World").asString(context)

        assertEquals("Hello World", result)
    }

    @Test
    fun `StringResource asString with context and no args resolves through context getString`() {
        val context = mockk<Context>()
        every { context.getString(TEST_RES_ID_PLAIN, *emptyArray<Any>()) } returns "Plain"

        val result = UiText.StringResource(TEST_RES_ID_PLAIN).asString(context)

        assertEquals("Plain", result)
    }
}
