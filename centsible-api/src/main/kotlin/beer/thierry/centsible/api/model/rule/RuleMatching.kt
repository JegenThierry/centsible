package beer.thierry.centsible.api.model.rule

import beer.thierry.centsible.api.model.category.CategoryType
import java.math.BigDecimal
import java.util.UUID

data class RuleContext(
    val description: String,
    val amount: BigDecimal,
    val type: CategoryType,
    val accountId: UUID,
)

data class RuleEffects(
    val categoryId: Long? = null,
    val tagIds: Set<Long> = emptySet(),
) {
    val hasEffect: Boolean get() = categoryId != null || tagIds.isNotEmpty()
}

object RuleMatching {

    val VALID_OPERATORS: Map<RuleField, Set<RuleOperator>> = mapOf(
        RuleField.DESCRIPTION to setOf(RuleOperator.CONTAINS, RuleOperator.EQUALS, RuleOperator.STARTS_WITH),
        RuleField.AMOUNT to setOf(RuleOperator.GT, RuleOperator.GTE, RuleOperator.LT, RuleOperator.LTE, RuleOperator.EQUALS),
        RuleField.DIRECTION to setOf(RuleOperator.IS),
        RuleField.ACCOUNT to setOf(RuleOperator.IS),
    )

    /** Whether a single [condition] holds for [context]; an operator invalid for the field never matches. */
    fun conditionMatches(condition: RuleConditionDTO, context: RuleContext): Boolean = when (condition.field) {
        RuleField.DESCRIPTION -> when (condition.operator) {
            RuleOperator.CONTAINS -> context.description.contains(condition.value, ignoreCase = true)
            RuleOperator.EQUALS -> context.description.equals(condition.value, ignoreCase = true)
            RuleOperator.STARTS_WITH -> context.description.startsWith(condition.value, ignoreCase = true)
            else -> false
        }

        RuleField.AMOUNT -> {
            val target = condition.value.toBigDecimalOrNull() ?: return false
            val cmp = context.amount.compareTo(target)
            when (condition.operator) {
                RuleOperator.GT -> cmp > 0
                RuleOperator.GTE -> cmp >= 0
                RuleOperator.LT -> cmp < 0
                RuleOperator.LTE -> cmp <= 0
                RuleOperator.EQUALS -> cmp == 0
                else -> false
            }
        }

        RuleField.DIRECTION -> condition.operator == RuleOperator.IS &&
            context.type.name.equals(condition.value, ignoreCase = true)

        RuleField.ACCOUNT -> condition.operator == RuleOperator.IS &&
            context.accountId.toString().equals(condition.value, ignoreCase = true)
    }

    /** Whether [conditions] hold for [context] under [matchAll] (all vs any). No conditions never matches. */
    fun matches(matchAll: Boolean, conditions: List<RuleConditionDTO>, context: RuleContext): Boolean {
        if (conditions.isEmpty()) return false
        return if (matchAll) conditions.all { conditionMatches(it, context) }
        else conditions.any { conditionMatches(it, context) }
    }

    /** A rule matches when (matchAll) all / (else) any of its conditions hold. No conditions never matches. */
    fun ruleMatches(rule: RuleDTO, context: RuleContext): Boolean =
        matches(rule.matchAll, rule.conditions, context)

    /**
     * Evaluates [rules] (expected in priority order — highest first) against [context]. The first
     * matching rule whose SET_CATEGORY action targets a category of the transaction's own type wins
     * the category; ADD_TAG actions from every matching rule accumulate. Disabled rules are skipped.
     */
    fun evaluate(rules: List<RuleDTO>, context: RuleContext): RuleEffects {
        var categoryId: Long? = null
        val tagIds = LinkedHashSet<Long>()
        for (rule in rules) {
            if (!rule.enabled || !ruleMatches(rule, context)) continue
            for (action in rule.actions) {
                when (action.type) {
                    RuleActionType.SET_CATEGORY -> {
                        val category = action.category
                        if (categoryId == null && category?.id != null && category.type == context.type) {
                            categoryId = category.id
                        }
                    }

                    RuleActionType.ADD_TAG -> action.tag?.id?.let { tagIds.add(it) }
                }
            }
        }
        return RuleEffects(categoryId, tagIds)
    }
}
