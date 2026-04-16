package beer.thierry.budgetplanner.api.model.category

enum class CategoryType(val value: String) {
    INCOME("INCOME"),
    EXPENSE("EXPENSE");

    companion object {
        fun fromValue(value: String): CategoryType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException("Unknown value: $value")
        }
    }
}
