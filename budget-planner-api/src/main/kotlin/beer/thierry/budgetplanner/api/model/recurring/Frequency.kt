package beer.thierry.budgetplanner.api.model.recurring

import java.time.LocalDate

enum class Frequency {
    DAILY, WEEKLY, MONTHLY, YEARLY;

    fun advance(from: LocalDate): LocalDate = when (this) {
        DAILY -> from.plusDays(1)
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
        YEARLY -> from.plusYears(1)
    }

    companion object {
        fun fromValue(value: String): Frequency =
            entries.firstOrNull { it.name == value.uppercase() }
                ?: throw IllegalArgumentException("Unknown frequency: $value")
    }
}
