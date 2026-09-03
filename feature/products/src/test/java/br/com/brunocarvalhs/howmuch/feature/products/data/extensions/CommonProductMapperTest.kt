package br.com.brunocarvalhs.howmuch.feature.products.data.extensions

import br.com.brunocarvalhs.howmuch.feature.products.data.model.CommonProductModel
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class CommonProductMapperTest {

    @Test
    fun `toDomain maps every field`() {
        val model = CommonProductModel(id = "1", name = "Arroz", category = "Mercearia", unit = "kg")

        val result = model.toDomain()

        assertEquals(CommonProduct(id = "1", name = "Arroz", category = "Mercearia", unit = "kg"), result)
    }

    @Test
    fun `toModel maps every field`() {
        val domain = CommonProduct(id = "1", name = "Arroz", category = "Mercearia", unit = "kg")

        val result = domain.toModel()

        assertEquals(CommonProductModel(id = "1", name = "Arroz", category = "Mercearia", unit = "kg"), result)
    }
}
