package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class FirebaseAuthUiConfigTest {

    private val context = mockk<Context>(relaxed = true)
    private val googleProvider = GoogleAuthProviderFactory()
    private val useCase = FirebaseAuthUiConfig(context, googleProvider)

    @Test
    fun `invoke builds a FirebaseUI configuration with the Google provider`() {
        val configuration = useCase()

        assertNotNull(configuration)
    }
}
