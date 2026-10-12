package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMethodTest {

    /** Remote values by flag key; an absent key behaves like an unfetched flag (the call-site default). */
    private fun flags(vararg remote: Pair<String, Boolean>) = object : FeatureFlagService {
        override fun isEnabled(key: String, default: Boolean) = remote.toMap()[key] ?: default
    }

    @Test
    fun `with nothing fetched only Google and e-mail show, so the Play reviewer can still sign in`() {
        assertEquals(listOf(AuthMethod.GOOGLE, AuthMethod.EMAIL), enabledAuthMethods(flags()))
    }

    @Test
    fun `new providers stay hidden until their flag is turned on`() {
        val methods = enabledAuthMethods(
            flags(AuthMethod.PHONE.flagKey to true, AuthMethod.GITHUB.flagKey to true)
        )

        assertEquals(
            listOf(AuthMethod.GOOGLE, AuthMethod.EMAIL, AuthMethod.PHONE, AuthMethod.GITHUB),
            methods
        )
    }

    @Test
    fun `a flag turned off hides that provider`() {
        val methods = enabledAuthMethods(flags(AuthMethod.EMAIL.flagKey to false))

        assertEquals(listOf(AuthMethod.GOOGLE), methods)
    }

    @Test
    fun `every flag off falls back to Google`() {
        val allOff = flags(*AuthMethod.entries.map { it.flagKey to false }.toTypedArray())

        assertEquals(listOf(AuthMethod.GOOGLE), enabledAuthMethods(allOff))
    }

    @Test
    fun `a provider whose setup is missing is dropped even with its flag on`() {
        val facebookOn = flags(AuthMethod.FACEBOOK.flagKey to true)

        val methods = enabledAuthMethods(facebookOn) { it != AuthMethod.FACEBOOK }

        assertEquals(listOf(AuthMethod.GOOGLE, AuthMethod.EMAIL), methods)
    }

    @Test
    fun `only Google and e-mail default to on`() {
        assertEquals(
            listOf(AuthMethod.GOOGLE, AuthMethod.EMAIL),
            AuthMethod.entries.filter { it.defaultEnabled }
        )
    }
}
