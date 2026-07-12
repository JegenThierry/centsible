package beer.thierry.centsible.api.model.rule

/** What a rule does to a matching transaction. */
enum class RuleActionType {
    SET_CATEGORY, ADD_TAG;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): RuleActionType =
            entries.find { it.name == value } ?: throw IllegalArgumentException("Unknown rule action type: $value")
    }
}
