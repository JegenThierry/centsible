package beer.thierry.centsible.core.services.recurring

import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import java.time.LocalDate

/**
 * Occurrence dates of [rule] within [windowStart]..[windowEnd] inclusive, following the cadence from
 * `nextRunAt` and respecting the rule's optional end date. [cap] is a loop backstop so a runaway
 * cadence can never generate an unbounded list — each caller sizes it to its own horizon.
 *
 * Shared by the notification shortfall projection, the net-worth forecast and safe-to-spend so all
 * three walk a rule's calendar the same way instead of each re-spelling the stepping loop.
 */
internal fun occurrenceDatesInWindow(
    rule: RecurringTransactionDTO,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    cap: Int,
): List<LocalDate> {
    val frequency = rule.frequency ?: return emptyList()
    var date = rule.nextRunAt ?: return emptyList()
    val hardEnd = rule.endDate
    val dates = mutableListOf<LocalDate>()
    var guard = 0
    while (!date.isAfter(windowEnd) && guard < cap) {
        if (hardEnd != null && date.isAfter(hardEnd)) break
        if (!date.isBefore(windowStart)) dates += date
        date = frequency.advance(date)
        guard++
    }
    return dates
}
