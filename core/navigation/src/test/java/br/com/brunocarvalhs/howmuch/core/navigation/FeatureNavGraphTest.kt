package br.com.brunocarvalhs.howmuch.core.navigation

import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureNavGraphTest {

    @Test
    fun `screenNames defaults to empty so existing graphs keep compiling`() {
        val graph = object : FeatureNavGraph {}

        assertTrue(graph.screenNames.isEmpty())
    }
}
