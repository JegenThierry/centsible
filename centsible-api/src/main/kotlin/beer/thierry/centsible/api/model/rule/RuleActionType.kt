package beer.thierry.centsible.api.model.rule

enum class RuleActionType {
    SET_CATEGORY, ADD_TAG;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): RuleActionType = valueOf(value)
    }
}
