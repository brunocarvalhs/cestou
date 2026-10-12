package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui.GoogleIdTokenProvider
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LinkWearViewModelTest {

    private val context = mockk<Context>()
    private val wearableSyncService = mockk<WearableSyncService>(relaxed = true)
    private val authService = mockk<AuthService> {
        every { currentUser } returns AuthenticatedUser(id = "user-1", email = "bruno@test.com")
    }
    private val googleIdTokenProvider = mockk<GoogleIdTokenProvider>()
    private val featureFlagService = mockk<FeatureFlagService> {
        every { isEnabled(any(), any()) } returns true
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { wearableSyncService.findNodeByPairingCode("123456") } returns "node-1"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun credential(email: String) = mockk<GoogleIdTokenCredential> {
        every { id } returns email
        every { idToken } returns "id-token"
    }

    private fun link(): LinkWearViewModel = LinkWearViewModel(
        wearableSyncService, authService, googleIdTokenProvider, featureFlagService
    ).apply {
        intent.onCodeChange("123456")
        intent.onLinkClick(context)
    }

    @Test
    fun `sends the uid and Google ID token to the watch`() {
        coEvery { googleIdTokenProvider.get(context) } returns credential("Bruno@test.com")

        val viewModel = link()

        assertTrue(viewModel.uiState.value.isSuccess)
        coVerify { wearableSyncService.sendAuthPairToNode("node-1", "user-1", "id-token") }
    }

    @Test
    fun `refuses an e-mail account that has no Google credential`() {
        coEvery { googleIdTokenProvider.get(context) } returns null

        val viewModel = link()

        assertEquals("O relógio só funciona com conta Google", viewModel.uiState.value.error)
        coVerify(exactly = 0) { wearableSyncService.sendAuthPairToNode(any(), any(), any()) }
    }

    @Test
    fun `refuses a Google account other than the signed-in one`() {
        coEvery { googleIdTokenProvider.get(context) } returns credential("other@test.com")

        val viewModel = link()

        assertEquals("Escolha a mesma conta Google que você usa no app", viewModel.uiState.value.error)
        coVerify(exactly = 0) { wearableSyncService.sendAuthPairToNode(any(), any(), any()) }
    }

    @Test
    fun `does nothing when the wear login flag is off`() {
        every { featureFlagService.isEnabled(any(), any()) } returns false

        val viewModel = link()

        assertEquals("Relógio indisponível no momento", viewModel.uiState.value.error)
        coVerify(exactly = 0) { wearableSyncService.findNodeByPairingCode(any()) }
    }
}
