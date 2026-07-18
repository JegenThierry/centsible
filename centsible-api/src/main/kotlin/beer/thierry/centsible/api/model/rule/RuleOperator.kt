package beer.thierry.centsible.api.model.rule

enum class RuleOperator {
    CONTAINS, EQUALS, STARTS_WITH, GT, GTE, LT, LTE, IS;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): RuleOperator = valueOf(value)
    }
}
