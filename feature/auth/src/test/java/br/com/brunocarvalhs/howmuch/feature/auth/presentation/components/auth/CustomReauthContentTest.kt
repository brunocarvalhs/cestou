package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.reauth.ReauthContentState
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real do
// conteúdo de reautenticação sem depender de emulador. FirebaseUser é mockado (mockk) seguindo
// a mesma convenção já usada em FirebaseAuthServiceTest/EmailAuthRepositoryImplTest, já que
// ReauthContentState.user exige uma instância real do SDK sem um valor default.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CustomReauthContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val user = mockk<FirebaseUser>(relaxed = true)
    private val googleProvider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)
    private val phoneProvider = AuthProvider.Phone(
        defaultNumber = null,
        defaultCountryCode = null,
        allowedCountries = null,
    )

    private fun string(resId: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resId)

    @Test
    fun `shows a default reason when none is provided`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomReauthContent(state = ReauthContentState(user = user))
            }
        }

        composeTestRule.onNodeWithText(string(R.string.reauth_title)).assertExists()
    }

    @Test
    fun `shows the given reason when provided`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomReauthContent(
                    state = ReauthContentState(user = user, reason = "Confirme para excluir sua conta"),
                )
            }
        }

        composeTestRule.onNodeWithText("Confirme para excluir sua conta").assertExists()
    }

    @Test
    fun `renders a button for each available provider`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomReauthContent(
                    state = ReauthContentState(user = user, providers = listOf(googleProvider, phoneProvider)),
                )
            }
        }

        composeTestRule.onNodeWithText("Continuar com Google").assertExists()
        composeTestRule.onNodeWithText("Entrar com Telefone").assertExists()
    }

    @Test
    fun `selecting a provider reports it back`() {
        var selected: AuthProvider? = null

        composeTestRule.setContent {
            CestouTheme {
                CustomReauthContent(
                    state = ReauthContentState(
                        user = user,
                        providers = listOf(googleProvider),
                        onProviderSelected = { selected = it },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Continuar com Google").performClick()

        assertEquals(googleProvider, selected)
    }

    @Test
    fun `shows the error message when present`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomReauthContent(state = ReauthContentState(user = user, error = "Falha ao reautenticar"))
            }
        }

        composeTestRule.onNodeWithText("Falha ao reautenticar").assertExists()
    }

    @Test
    fun `dismissing reports the intent`() {
        var dismissed = false

        composeTestRule.setContent {
            CestouTheme {
                CustomReauthContent(state = ReauthContentState(user = user, onDismiss = { dismissed = true }))
            }
        }

        composeTestRule.onNodeWithText(string(CoreR.string.action_cancel)).performClick()

        assertEquals(true, dismissed)
    }
}
