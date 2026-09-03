package br.com.brunocarvalhs.howmuch.core.ui.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductCategoryTest {

    @Test
    fun `fromString returns OUTROS for null input`() {
        assertEquals(ProductCategory.OUTROS, ProductCategory.fromString(null))
    }

    @Test
    fun `fromString matches exact enum name case-insensitively`() {
        assertEquals(ProductCategory.BEBIDAS, ProductCategory.fromString("bebidas"))
        assertEquals(ProductCategory.CARNES, ProductCategory.fromString("CARNES"))
    }

    @Test
    fun `fromString matches by keyword when the value is not an exact enum name`() {
        assertEquals(ProductCategory.LEGUMES, ProductCategory.fromString("legumes frescos"))
        assertEquals(ProductCategory.PERECIVEIS, ProductCategory.fromString("produto perecivível"))
        assertEquals(ProductCategory.CONGELADOS, ProductCategory.fromString("frozen food"))
        assertEquals(ProductCategory.HORTIFRUTI, ProductCategory.fromString("fresh fruit"))
        assertEquals(ProductCategory.LATICINIOS, ProductCategory.fromString("whole milk"))
        assertEquals(ProductCategory.LIMPEZA, ProductCategory.fromString("clean supplies"))
        assertEquals(ProductCategory.HIGIENE, ProductCategory.fromString("hygiene kit"))
        assertEquals(ProductCategory.MERCEARIA, ProductCategory.fromString("grocery item"))
        assertEquals(ProductCategory.PADARIA, ProductCategory.fromString("fresh bread"))
    }

    @Test
    fun `fromString falls back to OUTROS when nothing matches`() {
        assertEquals(ProductCategory.OUTROS, ProductCategory.fromString("something unrelated"))
    }

    @Test
    fun `fromString prioritizes keyword match over blank fallback for empty string`() {
        assertEquals(ProductCategory.OUTROS, ProductCategory.fromString(""))
    }
}
