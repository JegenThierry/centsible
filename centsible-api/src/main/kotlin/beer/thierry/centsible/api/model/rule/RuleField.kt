package beer.thierry.centsible.api.model.rule

/** A transaction attribute a rule condition can test. Valid operators per field live in [RuleMatching]. */
enum class RuleField {
    DESCRIPTION, AMOUNT, DIRECTION, ACCOUNT;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): RuleField =
            entries.find { it.name == value } ?: throw IllegalArgumentException("Unknown rule field: $value")
    }
}
