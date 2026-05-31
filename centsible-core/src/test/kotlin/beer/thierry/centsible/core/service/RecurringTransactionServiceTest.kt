package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.core.services.recurring.RecurringTransactionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class RecurringTransactionServiceTest {

    // Mockito's `any()` returns null which Kotlin's non-null types reject; reify the type.
    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock
    private lateinit var repository: IRecurringTransactionRepository

    @Mock
    private lateinit var categoriesRepository: ICategoriesRepository

    @InjectMocks
    private lateinit var service: RecurringTransactionService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
    private val accountId = UUID.randomUUID()
    private val ownedCategoryId = 1L
    private val foreignCategoryId = 99L

    private fun form(categoryId: Long) = RecurringTransactionForm(
        amount = BigDecimal("10.00"),
        categoryId = categoryId,
        description = "Rent",
        frequency = Frequency.MONTHLY,
        startDate = LocalDate.now(),
    )

    private fun stubOwned(categoryId: Long) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(mapOf(categoryId to CategoryClassification(CategoryType.EXPENSE, false)))
    }

    private fun stubNotOwned(categoryId: Long) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(emptyMap())
    }

    @Test
    fun `create with an owned category persists the rule`() {
        val f = form(ownedCategoryId)
        val created = RecurringTransactionDTO(id = UUID.randomUUID())
        stubOwned(ownedCategoryId)
        `when`(repository.create(accountId, f, user)).thenReturn(created)

        val result = service.create(accountId, f, user)

        assertEquals(created, result)
        verify(repository).create(accountId, f, user)
    }

    @Test
    fun `create with another user's category is rejected and never hits the repository`() {
        val f = form(foreignCategoryId)
        stubNotOwned(foreignCategoryId)

        assertThrows(IllegalArgumentException::class.java) {
            service.create(accountId, f, user)
        }

        verify(repository, never()).create(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `update with another user's category is rejected and never hits the repository`() {
        val f = form(foreignCategoryId)
        stubNotOwned(foreignCategoryId)

        assertThrows(IllegalArgumentException::class.java) {
            service.update(UUID.randomUUID(), f, user)
        }

        verify(repository, never()).update(anyArg(), anyArg(), anyArg())
    }
}
