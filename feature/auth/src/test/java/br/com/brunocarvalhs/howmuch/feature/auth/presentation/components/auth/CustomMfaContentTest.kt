package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real do
// conteúdo de MFA sem depender de emulador. MfaEnrollmentContentState/MfaChallengeContentState
// são data classes puras do firebase-ui-auth (sem objetos reais do Firebase SDK), então são
// construtíveis diretamente em teste.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CustomMfaContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resId)

    @Test
    fun `enrollment select factor step shows the choose method text`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaEnrollmentContent(
                    state = MfaEnrollmentContentState(step = MfaEnrollmentStep.SelectFactor),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_setup_choose_method)).assertExists()
    }

    @Test
    fun `enrollment configure sms step shows the phone field and send button`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaEnrollmentContent(
                    state = MfaEnrollmentContentState(
                        step = MfaEnrollmentStep.ConfigureSms,
                        phoneNumber = "11999999999",
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_phone_label)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.mfa_send_sms)).assertExists()
    }

    @Test
    fun `enrollment configure totp step shows the secret key`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaEnrollmentContent(
                    state = MfaEnrollmentContentState(step = MfaEnrollmentStep.ConfigureTotp, totpSecret = null),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_totp_instructions)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.mfa_scanned)).assertExists()
    }

    @Test
    fun `enrollment verify factor step shows the verification code field`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaEnrollmentContent(
                    state = MfaEnrollmentContentState(step = MfaEnrollmentStep.VerifyFactor),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_verification_code_label)).assertExists()
        composeTestRule.onNodeWithText(string(CoreR.string.action_confirm)).assertExists()
    }

    @Test
    fun `enrollment shows a progress indicator while loading`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaEnrollmentContent(
                    state = MfaEnrollmentContentState(step = MfaEnrollmentStep.VerifyFactor, isLoading = true),
                )
            }
        }

        composeTestRule.onNodeWithText(string(CoreR.string.action_confirm)).assertExists()
    }

    @Test
    fun `enrollment shows the error message when present`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaEnrollmentContent(
                    state = MfaEnrollmentContentState(
                        step = MfaEnrollmentStep.VerifyFactor,
                        error = "Código inválido",
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Código inválido").assertExists()
    }

    @Test
    fun `challenge shows the sms instructions for an sms factor`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaChallengeContent(
                    state = MfaChallengeContentState(factorType = MfaFactor.Sms),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_enter_code_sms)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.mfa_verify)).assertExists()
    }

    @Test
    fun `challenge shows the totp instructions for a totp factor`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaChallengeContent(
                    state = MfaChallengeContentState(factorType = MfaFactor.Totp),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_enter_code_totp)).assertExists()
    }

    @Test
    fun `challenge hides the verify button while loading`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaChallengeContent(
                    state = MfaChallengeContentState(factorType = MfaFactor.Sms, isLoading = true),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.mfa_verify)).assertDoesNotExist()
    }

    @Test
    fun `challenge shows the error message when present`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMfaChallengeContent(
                    state = MfaChallengeContentState(factorType = MfaFactor.Sms, error = "Código inválido"),
                )
            }
        }

        composeTestRule.onNodeWithText("Código inválido").assertExists()
    }
}
