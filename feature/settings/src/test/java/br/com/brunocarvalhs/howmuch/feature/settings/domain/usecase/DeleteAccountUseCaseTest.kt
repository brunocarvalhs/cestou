package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.exception.RecentLoginRequiredException
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountUseCaseTest {

    private val authService = mockk<AuthService>()
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val userRepository = mockk<UserRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val useCase = DeleteAccountUseCase(
        authService,
        shoppingRepository,
        userRepository,
        settingsRepository
    )

    private fun ownedList(id: String, users: List<String>) = Shopping(
        id = id,
        title = "List $id",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = users,
        roles = emptyMap()
    )

    @Test
    fun `invoke deletes solo-owned lists, leaves shared ones, deletes profile and account`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { authService.hasRecentLogin() } returns true
        val solo = ownedList("1", listOf("u1"))
        val shared = ownedList("2", listOf("u1", "u2"))
        coEvery { shoppingRepository.getAll() } returns listOf(solo, shared)
        coEvery { shoppingRepository.delete(solo) } returns Unit
        coEvery { shoppingRepository.leave("2", "u1") } returns Unit
        coEvery { userRepository.deleteProfile("u1") } returns Result.success(Unit)
        coEvery { settingsRepository.deleteAllData() } returns Unit
        coEvery { authService.deleteAccount() } returns Result.success(Unit)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify { shoppingRepository.delete(solo) }
        coVerify { shoppingRepository.leave("2", "u1") }
        coVerify(exactly = 0) { shoppingRepository.update(any()) }
        coVerify { userRepository.deleteProfile("u1") }
        coVerify { settingsRepository.deleteAllData() }
        coVerify { authService.deleteAccount() }
    }

    @Test
    fun `invoke does not delete the account when leaving a shared list fails`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { authService.hasRecentLogin() } returns true
        coEvery { shoppingRepository.getAll() } returns listOf(ownedList("2", listOf("u1", "u2")))
        coEvery { shoppingRepository.leave("2", "u1") } throws IllegalStateException("permission-denied")

        val result = useCase()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { userRepository.deleteProfile(any()) }
        coVerify(exactly = 0) { authService.deleteAccount() }
    }

    @Test
    fun `invoke fails when there is no authenticated user`() = runTest {
        every { authService.currentUser } returns null

        val result = useCase()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { shoppingRepository.getAll() }
    }

    @Test
    fun `invoke fails when deleting the Firebase account fails`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { authService.hasRecentLogin() } returns true
        coEvery { shoppingRepository.getAll() } returns emptyList()
        coEvery { userRepository.deleteProfile("u1") } returns Result.success(Unit)
        coEvery { settingsRepository.deleteAllData() } returns Unit
        coEvery { authService.deleteAccount() } returns Result.failure(IllegalStateException("boom"))

        val result = useCase()

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke deletes nothing when the sign-in is not recent`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { authService.hasRecentLogin() } returns false

        val result = useCase()

        assertTrue(result.exceptionOrNull() is RecentLoginRequiredException)
        coVerify(exactly = 0) { shoppingRepository.getAll() }
        coVerify(exactly = 0) { userRepository.deleteProfile(any()) }
        coVerify(exactly = 0) { settingsRepository.deleteAllData() }
        coVerify(exactly = 0) { authService.deleteAccount() }
    }
}
