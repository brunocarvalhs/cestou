package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FirebaseAuthUiConfigTest {

    private val context = mockk<Context>(relaxed = true)
    private val flags = mockk<FeatureFlagService>()

    private fun config(enabled: Set<AuthMethod>): FirebaseAuthUiConfig {
        every { flags.isEnabled(any(), any()) } answers {
            AuthMethod.entries.first { it.flagKey == firstArg<String>() } in enabled
        }
        return FirebaseAuthUiConfig(context, GoogleAuthProviderFactory(), flags)
    }

    @Test
    fun `invoke builds a FirebaseUI configuration with the Google provider`() {
        assertNotNull(config(setOf(AuthMethod.GOOGLE, AuthMethod.EMAIL))())
    }

    @Test
    fun `phone, Apple, Microsoft and GitHub register their FirebaseUI providers when enabled`() {
        val config = config(
            setOf(AuthMethod.EMAIL, AuthMethod.PHONE, AuthMethod.APPLE, AuthMethod.MICROSOFT, AuthMethod.GITHUB)
        )

        assertNotNull(config())
        assertEquals(
            listOf("google.com", "phone", "apple.com", "microsoft.com", "github.com"),
            config.providers.map { it.providerId }
        )
    }

    @Test
    fun `Facebook is dropped while its app id is not configured, even with the flag on`() {
        val config = config(setOf(AuthMethod.GOOGLE, AuthMethod.FACEBOOK))

        assertEquals(listOf(AuthMethod.GOOGLE), config.methods)
    }

    @Test
    fun `e-mail only still keeps Google registered so FirebaseUI does not skip the picker`() {
        val config = config(setOf(AuthMethod.EMAIL))

        assertEquals(listOf(AuthMethod.EMAIL), config.methods)
        assertEquals(listOf("google.com"), config.providers.map { it.providerId })
    }
}
