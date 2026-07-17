package beer.thierry.centsible.api.model.rule

/**
 * Comparison applied by a rule condition. Which operators are valid depends on the field:
 * text fields use CONTAINS/EQUALS/STARTS_WITH, AMOUNT uses GT/GTE/LT/LTE/EQUALS, and the
 * enum-like fields (DIRECTION, ACCOUNT) use IS. Enforced in [RuleMatching] and validated on write.
 */
enum class RuleOperator {
    CONTAINS, EQUALS, STARTS_WITH, GT, GTE, LT, LTE, IS;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): RuleOperator = valueOf(value)
    }
}
