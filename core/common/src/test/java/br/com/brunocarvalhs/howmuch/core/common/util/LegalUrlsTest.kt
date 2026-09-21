package br.com.brunocarvalhs.howmuch.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalUrlsTest {

    @Test
    fun `privacy policy points to the hosted legal page`() {
        assertEquals(
            "https://bruno-carvalho.dev.br/legal?doc=cestou-privacy-policy",
            LegalUrls.PRIVACY_POLICY_URL
        )
    }

    @Test
    fun `terms of use points to the hosted legal page`() {
        assertEquals(
            "https://bruno-carvalho.dev.br/legal?doc=cestou-terms-of-use",
            LegalUrls.TERMS_OF_USE_URL
        )
    }
}
