package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.rule.RuleActionDTO
import beer.thierry.centsible.api.model.rule.RuleActionForm
import beer.thierry.centsible.api.model.rule.RuleActionType
import beer.thierry.centsible.api.model.rule.RuleConditionDTO
import beer.thierry.centsible.api.model.rule.RuleConditionForm
import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleField
import beer.thierry.centsible.api.model.rule.RuleForm
import beer.thierry.centsible.api.model.rule.RuleOperator
import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.IRuleRepository
import beer.thierry.centsible.api.repository.ITagRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.repository.RuleCandidateTransaction
import beer.thierry.centsible.core.services.rule.RuleService
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
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RuleServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()
    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    @Mock private lateinit var rules: IRuleRepository
    @Mock private lateinit var categories: ICategoriesRepository
    @Mock private lateinit var tags: ITagRepository
    @Mock private lateinit var transactions: ITransactionRepository

    @InjectMocks private lateinit var service: RuleService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)

    private fun descCondition(value: String) = RuleConditionForm(RuleField.DESCRIPTION, RuleOperator.CONTAINS, value)
    private fun categoryActionForm(id: Long) = RuleActionForm(RuleActionType.SET_CATEGORY, categoryId = id)

    @Test
    fun `create rejects an invalid field-operator combination`() {
        val form = RuleForm(
            name = "bad",
            conditions = listOf(RuleConditionForm(RuleField.AMOUNT, RuleOperator.CONTAINS, "10")),
            actions = listOf(categoryActionForm(1L)),
        )
        assertThrows(LocalizedException::class.java) { service.create(user, form) }
        verify(rules, never()).create(anyArg(), anyArg())
    }

    @Test
    fun `create rejects a managed target category`() {
        `when`(categories.fetchCategoryClassifications(user, listOf(3L)))
            .thenReturn(mapOf(3L to CategoryClassification(CategoryType.EXPENSE, isManaged = true)))
        val form = RuleForm(
            name = "managed",
            conditions = listOf(descCondition("x")),
            actions = listOf(categoryActionForm(3L)),
        )
        assertThrows(LocalizedException::class.java) { service.create(user, form) }
        verify(rules, never()).create(anyArg(), anyArg())
    }

    @Test
    fun `applyToExisting retags eligible transactions and tags every match`() {
        val ruleId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val coffeeTx = candidate(accountId, "Coffee shop", categoryId = 1L, managed = false)
        val teaTx = candidate(accountId, "Tea house", categoryId = 1L, managed = false)
        val managedCoffeeTx = candidate(accountId, "Coffee beans", categoryId = 99L, managed = true)

        `when`(rules.fetchById(user, ruleId)).thenReturn(
            ruleDTO(
                conditions = listOf(RuleConditionDTO(UUID.randomUUID(), RuleField.DESCRIPTION, RuleOperator.CONTAINS, "coffee")),
                actions = listOf(
                    RuleActionDTO(UUID.randomUUID(), RuleActionType.SET_CATEGORY, category = CategoryDTO(id = 8L, type = CategoryType.EXPENSE)),
                    RuleActionDTO(UUID.randomUUID(), RuleActionType.ADD_TAG, tag = TagDTO(id = 2L)),
                ),
            )
        )
        `when`(transactions.fetchForRuleEvaluation(user)).thenReturn(listOf(coffeeTx, teaTx, managedCoffeeTx))

        val touched = service.applyToExisting(user, ruleId)

        assertEquals(2, touched)
        verify(transactions).setCategoryForTransactions(eqArg(user), eqArg(listOf(coffeeTx.id)), eqArg(8L))
        verify(tags).addTagsToTransactions(eqArg(user), eqArg(listOf(coffeeTx.id, managedCoffeeTx.id)), eqArg(listOf(2L)))
    }

    @Test
    fun `applyToExisting throws when the rule does not exist`() {
        val ruleId = UUID.randomUUID()
        `when`(rules.fetchById(user, ruleId)).thenReturn(null)
        assertThrows(LocalizedException::class.java) { service.applyToExisting(user, ruleId) }
    }

    @Test
    fun `preview counts matching transactions without mutating anything`() {
        val accountId = UUID.randomUUID()
        val coffeeShop = candidate(accountId, "Coffee shop", categoryId = 1L, managed = false)
        val teaHouse = candidate(accountId, "Tea house", categoryId = 1L, managed = false)
        val coffeeBeans = candidate(accountId, "Coffee beans", categoryId = 1L, managed = false)
        `when`(transactions.fetchForRuleEvaluation(user)).thenReturn(listOf(coffeeShop, teaHouse, coffeeBeans))

        val result = service.preview(
            user,
            RuleForm(name = "", conditions = listOf(descCondition("coffee")), actions = emptyList()),
        )

        assertEquals(2, result.matchedCount)
        assertEquals(2, result.sample.size)
        verify(transactions, never()).setCategoryForTransactions(anyArg(), anyArg(), org.mockito.ArgumentMatchers.anyLong())
        verify(tags, never()).addTagsToTransactions(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `preview ignores blank conditions and never queries transactions`() {
        val result = service.preview(
            user,
            RuleForm(name = "", conditions = listOf(descCondition("   ")), actions = emptyList()),
        )

        assertEquals(0, result.matchedCount)
        verify(transactions, never()).fetchForRuleEvaluation(anyArg())
    }

    private fun candidate(accountId: UUID, description: String, categoryId: Long, managed: Boolean) =
        RuleCandidateTransaction(
            id = UUID.randomUUID(),
            accountId = accountId,
            description = description,
            amount = BigDecimal("4.50"),
            type = CategoryType.EXPENSE,
            categoryId = categoryId,
            isManagedCategory = managed,
        )

    private fun ruleDTO(conditions: List<RuleConditionDTO>, actions: List<RuleActionDTO>) = RuleDTO(
        id = UUID.randomUUID(),
        name = "rule",
        matchAll = true,
        enabled = true,
        priority = 0,
        conditions = conditions,
        actions = actions,
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
    )
}
