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

class ShoppingUpdateUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val authService = mockk<AuthService>()
    private val useCase = ShoppingUpdateUseCase(repository, authService)

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
    fun `invoke writes only the given fields`() = runTest {
        coEvery { repository.updateFields("list1", mapOf("title" to "New title")) } returns Result.success(Unit)

        val result = useCase("list1", mapOf("title" to "New title"))

        assertTrue(result.isSuccess)
        coVerify { repository.updateFields("list1", mapOf("title" to "New title")) }
    }

    @Test
    fun `execute builds a fields map from AI agent arguments and writes it`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)

        val result = useCase.execute(
            arguments = mapOf(
                "shopping_id" to "list1",
                "title" to "Weekly Groceries",
                "description" to "desc",
                "price" to UPDATED_PRICE,
                "status" to "NEW"
            ),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.updateFields(
                "list1",
                mapOf(
                    "title" to "Weekly Groceries",
                    "description" to "desc",
                    "price" to UPDATED_PRICE,
                    "status" to "NEW"
                )
            )
        }
    }

    @Test
    fun `execute throws when shopping_id argument is missing`() {
        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(
                    arguments = emptyMap(),
                    session = mockk(relaxed = true),
                    metadata = emptyMap()
                )
            }
        }
    }

    @Test
    fun `execute throws when the shopping list does not exist`() {
        coEvery { repository.getById("missing") } returns null

        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(
                    arguments = mapOf("shopping_id" to "missing"),
                    session = mockk(relaxed = true),
                    metadata = emptyMap()
                )
            }
        }
    }

    @Test
    fun `execute only sends the field that was actually provided`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)

        val result = useCase.execute(
            arguments = mapOf(
                "shopping_id" to "list1",
                "price" to UPDATED_PRICE
            ),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify { repository.updateFields("list1", mapOf("price" to UPDATED_PRICE)) }
    }

    @Test
    fun `execute denies changing users or roles when caller is not the owner`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        every { authService.currentUser } returns AuthenticatedUser(id = MEMBER_ID)

        val result = useCase.execute(
            arguments = mapOf("shopping_id" to "list1", "roles" to mapOf(MEMBER_ID to User.Role.OWNER.name)),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OwnershipRequiredException)
        coVerify(exactly = 0) { repository.updateFields(any(), any()) }
    }

    @Test
    fun `execute allows changing users and roles when caller is the owner`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)
        every { authService.currentUser } returns AuthenticatedUser(id = OWNER_ID)

        val result = useCase.execute(
            arguments = mapOf(
                "shopping_id" to "list1",
                "users" to listOf(OWNER_ID, MEMBER_ID),
                "roles" to mapOf(OWNER_ID to User.Role.OWNER.name, MEMBER_ID to User.Role.EDITOR.name)
            ),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.updateFields(
                "list1",
                mapOf(
                    "users" to listOf(OWNER_ID, MEMBER_ID),
                    "roles" to mapOf(OWNER_ID to User.Role.OWNER.name, MEMBER_ID to User.Role.EDITOR.name)
                )
            )
        }
    }

    @Test
    fun `execute accepts a lowercase status from the LLM`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)

        val result = useCase.execute(
            arguments = mapOf("shopping_id" to "list1", "status" to "finish"),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify { repository.updateFields("list1", mapOf("status" to Shopping.Status.FINISH.name)) }
    }
}

private const val UPDATED_PRICE = 10.5
private const val OWNER_ID = "owner-user-id"
private const val MEMBER_ID = "member-user-id"
