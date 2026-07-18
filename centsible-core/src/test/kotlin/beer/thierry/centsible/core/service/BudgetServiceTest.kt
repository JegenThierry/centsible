package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.core.services.budget.BudgetService
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
import java.util.*

@ExtendWith(MockitoExtension::class)
class BudgetServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock
    private lateinit var repository: IBudgetRepository

    @Mock
    private lateinit var categoriesRepository: ICategoriesRepository

    @InjectMocks
    private lateinit var service: BudgetService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
    private val ownedCategoryId = 1L
    private val foreignCategoryId = 99L

    private fun stubOwned(categoryId: Long) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(mapOf(categoryId to CategoryClassification(CategoryType.EXPENSE, false)))
    }

    private fun stubNotOwned(categoryId: Long) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(emptyMap())
    }

    @Test
    fun `create with an owned category persists the budget`() {
        val form = BudgetForm(categoryId = ownedCategoryId, amountLimit = BigDecimal("100.00"))
        val created = BudgetDTO(id = UUID.randomUUID())
        stubOwned(ownedCategoryId)
        `when`(repository.create(form, user)).thenReturn(created)

        val result = service.create(form, user)

        assertEquals(created, result)
        verify(repository).create(form, user)
    }

    @Test
    fun `create rejects a duplicate category-and-period with a specific conflict`() {
        val form = BudgetForm(categoryId = ownedCategoryId, amountLimit = BigDecimal("100.00"))
        stubOwned(ownedCategoryId)
        `when`(repository.existsForCategoryAndPeriod(user, ownedCategoryId, form.periodType, null)).thenReturn(true)

        val ex = assertThrows(LocalizedException::class.java) {
            service.create(form, user)
        }

        assertEquals("error.budget.duplicate", ex.messageKey)
        verify(repository, never()).create(anyArg(), anyArg())
    }

    @Test
    fun `create with another user's category is rejected and never hits the repository`() {
        val form = BudgetForm(categoryId = foreignCategoryId, amountLimit = BigDecimal("100.00"))
        stubNotOwned(foreignCategoryId)

        assertThrows(LocalizedException::class.java) {
            service.create(form, user)
        }

        verify(repository, never()).create(anyArg(), anyArg())
    }

    @Test
    fun `update with another user's category is rejected and never hits the repository`() {
        val form = BudgetForm(categoryId = foreignCategoryId, amountLimit = BigDecimal("100.00"))
        stubNotOwned(foreignCategoryId)

        assertThrows(LocalizedException::class.java) {
            service.update(UUID.randomUUID(), form, user)
        }

        verify(repository, never()).update(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `suggestions returns positive average spend for expense categories only`() {
        `when`(categoriesRepository.fetchAllCategories(user)).thenReturn(
            listOf(
                CategoryDTO(id = 1L, type = CategoryType.EXPENSE),
                CategoryDTO(id = 2L, type = CategoryType.EXPENSE),
                CategoryDTO(id = 3L, type = CategoryType.INCOME),
            )
        )
        `when`(repository.suggestedAmounts(anyArg(), anyArg(), org.mockito.ArgumentMatchers.anyInt(), anyArg()))
            .thenReturn(mapOf(1L to BigDecimal("42.00"), 2L to BigDecimal.ZERO))

        val result = service.suggestions(user)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].categoryId)
        assertEquals(BigDecimal("42.00"), result[0].suggestedAmount)
    }

    @Test
    fun `bulkCreateSuggested skips covered and zero-spend categories`() {
        `when`(categoriesRepository.fetchAllCategories(user)).thenReturn(
            listOf(
                CategoryDTO(id = 1L, type = CategoryType.EXPENSE),
                CategoryDTO(id = 2L, type = CategoryType.EXPENSE),
                CategoryDTO(id = 3L, type = CategoryType.EXPENSE),
            )
        )
        `when`(repository.suggestedAmounts(anyArg(), anyArg(), org.mockito.ArgumentMatchers.anyInt(), anyArg()))
            .thenReturn(mapOf(1L to BigDecimal("30.00"), 2L to BigDecimal("50.00"), 3L to BigDecimal.ZERO))
        `when`(repository.budgetedCategoryIds(anyArg(), anyArg())).thenReturn(setOf(2L))
        `when`(repository.create(anyArg(), anyArg())).thenReturn(BudgetDTO(id = UUID.randomUUID()))

        val created = service.bulkCreateSuggested(user)

        assertEquals(1, created.size)
        verify(repository).create(anyArg(), anyArg())
    }
}
