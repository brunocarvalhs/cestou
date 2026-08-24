package br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase.GoogleProviderUseCase
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleProviderUseCaseTest {

    private val useCase = GoogleProviderUseCase()

    @Test
    fun `invoke returns a Google provider requesting the email scope`() {
        val provider = useCase()

        assertTrue(provider is AuthProvider.Google)
        assertEquals(listOf("email"), (provider as AuthProvider.Google).scopes)
    }
}
