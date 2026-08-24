package br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase.GoogleProviderUseCase
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthConfigUseCaseTest {

    private val context = mockk<Context>(relaxed = true)
    private val googleProvider = GoogleProviderUseCase()
    private val useCase = AuthConfigUseCase(context, googleProvider)

    @Test
    fun `invoke builds a FirebaseUI configuration with the Google provider`() {
        val configuration = useCase()

        assertNotNull(configuration)
    }
}
