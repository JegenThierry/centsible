package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.rule.RuleActionDTO
import beer.thierry.centsible.api.model.rule.RuleActionType
import beer.thierry.centsible.api.model.rule.RuleConditionDTO
import beer.thierry.centsible.api.model.rule.RuleContext
import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleField
import beer.thierry.centsible.api.model.rule.RuleMatching
import beer.thierry.centsible.api.model.rule.RuleOperator
import beer.thierry.centsible.api.model.tag.TagDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class RuleMatchingTest {

    private val accountId: UUID = UUID.randomUUID()
    private val otherAccountId: UUID = UUID.randomUUID()

    private fun ctx(
        description: String = "Netflix monthly",
        amount: BigDecimal = BigDecimal("15.99"),
        type: CategoryType = CategoryType.EXPENSE,
        account: UUID = accountId,
    ) = RuleContext(description, amount, type, account)

    private fun condition(field: RuleField, operator: RuleOperator, value: String) =
        RuleConditionDTO(UUID.randomUUID(), field, operator, value)

    private fun categoryAction(id: Long, type: CategoryType) =
        RuleActionDTO(UUID.randomUUID(), RuleActionType.SET_CATEGORY, category = CategoryDTO(id = id, type = type))

    private fun tagAction(id: Long) =
        RuleActionDTO(UUID.randomUUID(), RuleActionType.ADD_TAG, tag = TagDTO(id = id))

    private fun rule(
        conditions: List<RuleConditionDTO>,
        actions: List<RuleActionDTO>,
        matchAll: Boolean = true,
        enabled: Boolean = true,
        priority: Int = 0,
    ) = RuleDTO(
        id = UUID.randomUUID(),
        name = "rule",
        matchAll = matchAll,
        enabled = enabled,
        priority = priority,
        conditions = conditions,
        actions = actions,
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
    )

    @Test
    fun `description operators are case-insensitive`() {
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "netflix"), ctx(description = "NETFLIX Sub")))
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.DESCRIPTION, RuleOperator.STARTS_WITH, "net"), ctx(description = "Netflix")))
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.DESCRIPTION, RuleOperator.EQUALS, "netflix monthly"), ctx(description = "Netflix monthly")))
        assertFalse(RuleMatching.conditionMatches(condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "spotify"), ctx()))
    }

    @Test
    fun `amount comparisons handle bad values gracefully`() {
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.AMOUNT, RuleOperator.GTE, "15.99"), ctx(amount = BigDecimal("15.99"))))
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.AMOUNT, RuleOperator.GT, "10"), ctx(amount = BigDecimal("15.99"))))
        assertFalse(RuleMatching.conditionMatches(condition(RuleField.AMOUNT, RuleOperator.LT, "10"), ctx(amount = BigDecimal("15.99"))))
        // Non-numeric value never matches (rather than blowing up).
        assertFalse(RuleMatching.conditionMatches(condition(RuleField.AMOUNT, RuleOperator.EQUALS, "abc"), ctx()))
    }

    @Test
    fun `direction and account conditions`() {
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.DIRECTION, RuleOperator.IS, "EXPENSE"), ctx(type = CategoryType.EXPENSE)))
        assertFalse(RuleMatching.conditionMatches(condition(RuleField.DIRECTION, RuleOperator.IS, "INCOME"), ctx(type = CategoryType.EXPENSE)))
        assertTrue(RuleMatching.conditionMatches(condition(RuleField.ACCOUNT, RuleOperator.IS, accountId.toString()), ctx(account = accountId)))
        assertFalse(RuleMatching.conditionMatches(condition(RuleField.ACCOUNT, RuleOperator.IS, otherAccountId.toString()), ctx(account = accountId)))
    }

    @Test
    fun `matchAll requires all conditions, ANY requires one`() {
        val conditions = listOf(
            condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "netflix"),
            condition(RuleField.AMOUNT, RuleOperator.LT, "10"),
        )
        assertFalse(RuleMatching.ruleMatches(rule(conditions, emptyList(), matchAll = true), ctx(amount = BigDecimal("15.99"))))
        assertTrue(RuleMatching.ruleMatches(rule(conditions, emptyList(), matchAll = false), ctx(amount = BigDecimal("15.99"))))
    }

    @Test
    fun `a rule with no conditions never matches`() {
        assertFalse(RuleMatching.ruleMatches(rule(emptyList(), listOf(tagAction(1L))), ctx()))
    }

    @Test
    fun `evaluate takes the highest-priority category and unions every matching rule's tags`() {
        val rules = listOf(
            rule(
                listOf(condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "netflix")),
                listOf(categoryAction(5L, CategoryType.EXPENSE), tagAction(1L)),
                priority = 10,
            ),
            rule(
                listOf(condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "netflix")),
                listOf(categoryAction(9L, CategoryType.EXPENSE), tagAction(2L)),
                priority = 5,
            ),
        )
        val effects = RuleMatching.evaluate(rules, ctx())
        assertEquals(5L, effects.categoryId)
        assertEquals(setOf(1L, 2L), effects.tagIds)
    }

    @Test
    fun `evaluate refuses a category whose type does not match the transaction`() {
        val rules = listOf(
            rule(
                listOf(condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "salary")),
                listOf(categoryAction(7L, CategoryType.INCOME)),
            ),
        )
        assertNull(RuleMatching.evaluate(rules, ctx(description = "Salary", type = CategoryType.EXPENSE)).categoryId)
    }

    @Test
    fun `evaluate ignores disabled rules`() {
        val rules = listOf(
            rule(
                listOf(condition(RuleField.DESCRIPTION, RuleOperator.CONTAINS, "netflix")),
                listOf(categoryAction(5L, CategoryType.EXPENSE)),
                enabled = false,
            ),
        )
        assertFalse(RuleMatching.evaluate(rules, ctx()).hasEffect)
    }
}
