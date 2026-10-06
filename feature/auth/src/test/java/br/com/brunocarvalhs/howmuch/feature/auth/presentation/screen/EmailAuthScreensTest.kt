package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.CompleteNameIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.EmailSignInIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.EmailSignUpIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.PasswordResetIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.CompleteNameUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailSignInUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailSignUpUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordResetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Spec EPA-02..EPA-04, EPA-06, EPA-07 and EPA-10: UI contract by testTag (the app runs in EN and pt-BR). */
@Suppress("TooManyFunctions")
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EmailAuthScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val validSignUp = EmailSignUpUiState(
        givenName = "Ana", familyName = "Silva", email = "ana@test.com", password = "12345678"
    )

    // region sign up

    @Test
    fun `sign up shows the four required fields and no optional label`() {
        composeTestRule.setContent {
            CestouTheme { EmailSignUpScreen(state = EmailSignUpUiState(), intent = EmailSignUpIntent()) }
        }

        listOf("signup_given_name_field", "signup_family_name_field", "signup_email_field", "signup_password_field")
            .forEach { composeTestRule.onNodeWithTag(it).assertExists() }
        assertEquals(0, composeTestRule.onAllNodesWithText("optional", substring = true, ignoreCase = true)
            .fetchSemanticsNodes().size)
    }

    @Test
    fun `sign up button follows the state`() {
        composeTestRule.setContent {
            CestouTheme { EmailSignUpScreen(state = EmailSignUpUiState(), intent = EmailSignUpIntent()) }
        }
        composeTestRule.onNodeWithTag("signup_submit_button").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `sign up button is enabled for a valid form and submits`() {
        var submitted = false
        composeTestRule.setContent {
            CestouTheme {
                EmailSignUpScreen(state = validSignUp, intent = EmailSignUpIntent(onSubmit = { submitted = true }))
            }
        }

        composeTestRule.onNodeWithTag("signup_submit_button").performScrollTo().assertIsEnabled().performClick()

        assertTrue(submitted)
    }

    @Test
    fun `field errors are shown under the field that failed`() {
        composeTestRule.setContent {
            CestouTheme {
                EmailSignUpScreen(
                    state = validSignUp.copy(givenNameError = PersonNameError.NOT_A_NAME),
                    intent = EmailSignUpIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("signup_given_name_field_error").assertExists()
        composeTestRule.onNodeWithTag("signup_family_name_field_error").assertDoesNotExist()
    }

    @Test
    fun `e-mail in use offers a sign-in action carrying the typed e-mail`() {
        var handedOver: String? = null
        composeTestRule.setContent {
            CestouTheme {
                EmailSignUpScreen(
                    state = validSignUp.copy(emailError = EmailFieldError.ALREADY_IN_USE),
                    intent = EmailSignUpIntent(),
                    onGoToSignIn = { handedOver = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("signup_email_in_use_sign_in_action").performScrollTo().performClick()

        assertEquals("ana@test.com", handedOver)
    }

    @Test
    fun `typing reaches the intent`() {
        var typed = ""
        composeTestRule.setContent {
            CestouTheme {
                EmailSignUpScreen(
                    state = EmailSignUpUiState(),
                    intent = EmailSignUpIntent(onGivenNameChange = { typed = it })
                )
            }
        }

        composeTestRule.onNodeWithTag("signup_given_name_field").performTextInput("Ana")

        assertEquals("Ana", typed)
    }

    @Test
    fun `network failure is shown as a form error`() {
        composeTestRule.setContent {
            CestouTheme {
                EmailSignUpScreen(
                    state = validSignUp.copy(formError = EmailAuthError.NETWORK),
                    intent = EmailSignUpIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("signup_form_error").assertExists()
    }

    // endregion

    // region sign in

    @Test
    fun `sign in shows its fields, the generic error and both links`() {
        var forgotWith: String? = null
        var createAccount = false
        composeTestRule.setContent {
            CestouTheme {
                EmailSignInScreen(
                    state = EmailSignInUiState(
                        email = "ana@test.com", password = "x", formError = EmailAuthError.INVALID_CREDENTIALS
                    ),
                    intent = EmailSignInIntent(),
                    onForgotPassword = { forgotWith = it },
                    onCreateAccount = { createAccount = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("signin_email_field").assertExists()
        composeTestRule.onNodeWithTag("signin_password_field").assertExists()
        composeTestRule.onNodeWithTag("signin_form_error").assertExists()
        composeTestRule.onNodeWithTag("signin_forgot_password").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("signin_create_account").performScrollTo().performClick()

        assertEquals("ana@test.com", forgotWith)
        assertTrue(createAccount)
    }

    @Test
    fun `sign in button is disabled while loading`() {
        composeTestRule.setContent {
            CestouTheme {
                EmailSignInScreen(
                    state = EmailSignInUiState(email = "ana@test.com", password = "x", isLoading = true),
                    intent = EmailSignInIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("signin_submit_button").performScrollTo().assertIsNotEnabled()
    }

    // endregion

    // region password reset

    @Test
    fun `reset shows the confirmation and the resend countdown after sending`() {
        composeTestRule.setContent {
            CestouTheme {
                PasswordResetScreen(
                    state = PasswordResetUiState(email = "ana@test.com", isSent = true, resendCountdownSeconds = 12),
                    intent = PasswordResetIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("reset_sent_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset_submit_button").performScrollTo().assertIsNotEnabled()
        assertEquals(1, composeTestRule.onAllNodesWithText("12", substring = true).fetchSemanticsNodes().size)
    }

    // endregion

    // region required-name step

    @Test
    fun `name step has both fields, keeps continue disabled and offers another account`() {
        var usedAnotherAccount = false
        composeTestRule.setContent {
            CestouTheme {
                CompleteNameScreen(
                    state = CompleteNameUiState(givenName = "Ana", familyNameError = PersonNameError.EMPTY),
                    intent = CompleteNameIntent(onUseAnotherAccount = { usedAnotherAccount = true })
                )
            }
        }

        composeTestRule.onNodeWithTag("name_step_given_name_field").assertExists()
        composeTestRule.onNodeWithTag("name_step_family_name_field").assertExists()
        composeTestRule.onNodeWithTag("name_step_family_name_field_error").assertExists()
        composeTestRule.onNodeWithTag("name_step_submit_button").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("name_step_use_another_account").performScrollTo().performClick()

        assertTrue(usedAnotherAccount)
    }

    @Test
    fun `name step shows a save error`() {
        composeTestRule.setContent {
            CestouTheme {
                CompleteNameScreen(
                    state = CompleteNameUiState(givenName = "Ana", familyName = "Silva", hasSaveError = true),
                    intent = CompleteNameIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("name_step_save_error").assertExists()
    }

    // endregion
}
