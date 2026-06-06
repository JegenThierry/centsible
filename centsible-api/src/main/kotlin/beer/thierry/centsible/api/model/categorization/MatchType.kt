package beer.thierry.centsible.api.model.categorization

enum class MatchType {
    CONTAINS, EQUALS, STARTS_WITH;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): MatchType =
            entries.find { it.name == value } ?: throw IllegalArgumentException("Unknown match type: $value")
    }
}
