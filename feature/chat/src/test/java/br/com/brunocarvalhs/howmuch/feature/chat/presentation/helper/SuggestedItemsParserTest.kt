package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedItemsParserTest {

    @Test
    fun `extracts bulleted and numbered items`() {
        val text = "Para o churrasco:\n- Picanha\n* Carvão\n1. Pão de alho\n2) Cerveja\nBom apetite!"

        assertEquals(listOf("Picanha", "Carvão", "Pão de alho", "Cerveja"), parseSuggestedItems(text))
    }

    @Test
    fun `strips emphasis and trailing descriptions`() {
        val text = "- **Banana** - rica em potássio\n- *Maçã*: da estação"

        assertEquals(listOf("Banana", "Maçã"), parseSuggestedItems(text))
    }

    @Test
    fun `plain prose and overlong lines are not items`() {
        val long = "- " + "x".repeat(80)

        assertTrue(parseSuggestedItems("Sem lista aqui.\n$long").isEmpty())
    }

    @Test
    fun `recipe steps are not offered as items`() {
        val text = listOf(
            "1. Pré-aqueça o forno a 180 graus.",
            "2. Misture a farinha com os ovos e o leite.",
            "3. Asse por 40 minutos.",
        ).joinToString("\n")

        assertTrue(parseSuggestedItems(text).isEmpty())
    }

    @Test
    fun `a short list with a few sentences still offers the real items`() {
        val text = "- Arroz\n- Feijão\n- Misture tudo com cuidado até ficar homogêneo."

        assertEquals(listOf("Arroz", "Feijão"), parseSuggestedItems(text))
    }

    @Test
    fun `removes duplicates and caps at twenty items`() {
        val text = (1..30).joinToString("\n") { "- Item $it" } + "\n- item 1"

        val items = parseSuggestedItems(text)

        assertEquals(20, items.size)
        assertEquals("Item 1", items.first())
    }
}
