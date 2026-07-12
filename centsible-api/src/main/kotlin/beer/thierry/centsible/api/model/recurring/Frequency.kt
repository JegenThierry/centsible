package beer.thierry.centsible.api.model.recurring

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
        /** Parses [value] case-insensitively; throws [IllegalArgumentException] if it names no [Frequency]. */
        fun fromValue(value: String): Frequency =
            runCatching { valueOf(value.uppercase()) }
                .getOrElse { throw IllegalArgumentException("Unknown frequency: $value") }
    }
}
