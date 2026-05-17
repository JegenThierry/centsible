package beer.thierry.centsible.api.model.category

enum class CategoryType {
    INCOME, EXPENSE;

    val value: String get() = name

    companion object {
        fun fromValue(value: String): CategoryType =
            entries.find { it.value == value } ?: throw IllegalArgumentException("Unknown value: $value")
    }
}

