package br.com.brunocarvalhs.howmuch.feature.settings.presentation.res

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.feature.settings.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Keeps the in-app privacy copy in sync with docs/legal/privacy.html (Play Data safety review).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PrivacyContentTest {

    private fun content() =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.settings_privacy_content)

    private fun assertDisclosesEmailSignIn(anonymousClaim: String) {
        val text = content()
        assertTrue(text, text.contains("Firebase Authentication"))
        assertTrue(text, text.contains("brunocarvalhs@outlook.com.br"))
        assertFalse(text, text.contains(anonymousClaim))
        assertFalse(text, text.contains("suporte@cestou.com.br"))
    }

    @Test
    fun `english copy discloses email sign-in`() = assertDisclosesEmailSignIn("anonymous user ID")

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `portuguese copy discloses email sign-in`() = assertDisclosesEmailSignIn("ID de usuário anônimo")

    @Test
    @Config(qualifiers = "es")
    fun `spanish copy discloses email sign-in`() = assertDisclosesEmailSignIn("ID de usuario anónimo")
}
