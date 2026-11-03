package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.mapper

import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.CategorySpending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySpendingUiMapperTest {

    @Test
    fun `keys that resolve to the same category are merged`() {
        val shares = listOf(CategorySpending("dairy", 10.0), CategorySpending("laticínios", 20.0)).toCategoryShares()

        assertEquals(1, shares.size)
        assertEquals(ProductCategory.LATICINIOS, shares.single().category)
        assertEquals(30.0, shares.single().total, 0.0)
    }

    @Test
    fun `an unknown key falls into outros`() {
        val shares = listOf(CategorySpending("whatever", 5.0)).toCategoryShares()

        assertEquals(ProductCategory.OUTROS, shares.single().category)
    }

    @Test
    fun `sorted by value with the top five followed by an others bucket`() {
        val raw = listOf("carnes", "bebidas", "limpeza", "higiene", "padaria", "congelados", "hortifruti")
            .mapIndexed { i, key -> CategorySpending(key, (i + 1) * 10.0) }

        val shares = raw.toCategoryShares()

        assertEquals(6, shares.size)
        assertEquals(ProductCategory.HORTIFRUTI, shares.first().category)
        assertNull(shares.last().category)
        assertEquals(30.0, shares.last().total, 0.0)
    }

    @Test
    fun `no others bucket when everything fits in the top five`() {
        val shares = listOf(CategorySpending("carnes", 1.0), CategorySpending("bebidas", 2.0)).toCategoryShares()

        assertEquals(2, shares.size)
        assertTrue(shares.none { it.category == null })
    }

    @Test
    fun `percentages add up to one`() {
        val shares = listOf(
            CategorySpending("carnes", 50.0),
            CategorySpending("bebidas", 30.0),
            CategorySpending("limpeza", 20.0)
        ).toCategoryShares()

        assertEquals(1f, shares.sumOf { it.percent.toDouble() }.toFloat(), 0.001f)
        assertEquals(0.5f, shares.first().percent, 0.001f)
    }

    @Test
    fun `a zero total gives an empty list without dividing by zero`() {
        assertTrue(listOf(CategorySpending("carnes", 0.0)).toCategoryShares().isEmpty())
        assertTrue(emptyList<CategorySpending>().toCategoryShares().isEmpty())
    }

    @Test
    fun `ties keep a stable order`() {
        val shares = listOf(CategorySpending("bebidas", 10.0), CategorySpending("carnes", 10.0)).toCategoryShares()

        assertEquals(listOf(ProductCategory.CARNES, ProductCategory.BEBIDAS), shares.map { it.category })
    }
}
