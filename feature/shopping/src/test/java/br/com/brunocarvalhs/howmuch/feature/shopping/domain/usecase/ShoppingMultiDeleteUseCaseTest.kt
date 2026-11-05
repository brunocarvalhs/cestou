package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.exception.OwnershipRequiredException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingMultiDeleteUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val authService = mockk<AuthService>()
    private val useCase = ShoppingMultiDeleteUseCase(repository, authService)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = listOf(OWNER_ID),
        roles = mapOf(OWNER_ID to User.Role.OWNER.name)
    )

    @Test
    fun `invoke deletes the shopping list when the caller is the owner`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.delete(shopping) } returns Unit
        every { authService.currentUser } returns AuthenticatedUser(id = OWNER_ID)

        val result = useCase("list1")

        assertTrue(result.isSuccess)
        coVerify { repository.delete(shopping) }
    }

    @Test
    fun `invoke fails when the shopping list is not found`() = runTest {
        coEvery { repository.getById("missing") } returns null
        every { authService.currentUser } returns AuthenticatedUser(id = OWNER_ID)

        val result = useCase("missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke denies deletion when the caller is not the owner`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        every { authService.currentUser } returns AuthenticatedUser(id = MEMBER_ID)

        val result = useCase("list1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OwnershipRequiredException)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `execute resolves the shopping id from arguments and deletes it`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.delete(shopping) } returns Unit
        every { authService.currentUser } returns AuthenticatedUser(id = OWNER_ID)

        val result = useCase.execute(
            arguments = mapOf("shopping_id" to "list1"),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify { repository.delete(shopping) }
    }

    @Test
    fun `execute denies deletion when the caller is not the owner`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        every { authService.currentUser } returns AuthenticatedUser(id = MEMBER_ID)

        val result = useCase.execute(
            arguments = mapOf("shopping_id" to "list1"),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `execute throws when shopping_id argument is missing`() {
        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(arguments = emptyMap(), session = mockk(relaxed = true), metadata = emptyMap())
            }
        }
    }
}

private const val OWNER_ID = "owner-user-id"
private const val MEMBER_ID = "member-user-id"
