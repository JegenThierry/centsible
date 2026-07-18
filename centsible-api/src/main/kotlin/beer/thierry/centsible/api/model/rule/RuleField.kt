package beer.thierry.centsible.api.model.rule

enum class RuleField {
    DESCRIPTION, AMOUNT, DIRECTION, ACCOUNT;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): RuleField = valueOf(value)
    }
}
