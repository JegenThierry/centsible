package beer.thierry.centsible.core.services.reports

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.reports.SafeToSpendDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.api.services.reports.ISafeToSpendService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Deterministic discretionary-headroom calculator for the current month. Reuses the same
 * transfer-excluded aggregation the cash-flow report uses for booked actuals (ADR-0015), and projects
 * the remaining month from active, non-transfer recurring rules. No AI.
 */
@Service
class SafeToSpendService(
    private val reportsRepository: IReportsRepository,
    private val recurringRepository: IRecurringTransactionRepository,
) : ISafeToSpendService {

    override fun fetchForCurrentMonth(authenticatedUser: UserDTO): SafeToSpendDTO {
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val cashFlow = reportsRepository.fetchCashFlow(monthStart, monthEnd, authenticatedUser)
        val actualIncome = cashFlow.sumOf { it.income }
        val alreadySpent = cashFlow.sumOf { it.expense }

        var upcomingIncome = BigDecimal.ZERO
        var upcomingExpenses = BigDecimal.ZERO
        for (rule in recurringRepository.fetchAll(authenticatedUser)) {
            if (!rule.active || rule.isTransfer) continue
            val amount = rule.amount ?: continue
            val type = rule.type ?: continue
            val occurrences = occurrencesInWindow(rule, today, monthEnd)
            if (occurrences == 0) continue
            val total = amount.multiply(occurrences.toBigDecimal())
            when (type) {
                CategoryType.INCOME -> upcomingIncome += total
                CategoryType.EXPENSE -> upcomingExpenses += total
            }
        }

        val expectedIncome = actualIncome + upcomingIncome
        val safeToSpend = (expectedIncome - alreadySpent - upcomingExpenses).setScale(2, RoundingMode.HALF_UP)
        val daysRemaining = (monthEnd.dayOfMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        val dailyAllowance = safeToSpend.max(BigDecimal.ZERO)
            .divide(daysRemaining.toBigDecimal(), 2, RoundingMode.HALF_UP)

        return SafeToSpendDTO(
            yearMonth = month.toString(),
            currency = authenticatedUser.defaultCurrency,
            actualIncome = actualIncome.setScale(2, RoundingMode.HALF_UP),
            upcomingIncome = upcomingIncome.setScale(2, RoundingMode.HALF_UP),
            expectedIncome = expectedIncome.setScale(2, RoundingMode.HALF_UP),
            alreadySpent = alreadySpent.setScale(2, RoundingMode.HALF_UP),
            upcomingExpenses = upcomingExpenses.setScale(2, RoundingMode.HALF_UP),
            safeToSpend = safeToSpend,
            daysRemaining = daysRemaining,
            dailyAllowance = dailyAllowance,
        )
    }

    /** Counts recurring run dates within [windowStart, windowEnd], following the cadence from nextRunAt. */
    private fun occurrencesInWindow(
        rule: RecurringTransactionDTO,
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): Int {
        val frequency = rule.frequency ?: return 0
        val hardEnd = rule.endDate
        var date = rule.nextRunAt ?: return 0
        var count = 0
        var guard = 0
        while (!date.isAfter(windowEnd) && guard < MAX_OCCURRENCES) {
            if (hardEnd != null && date.isAfter(hardEnd)) break
            if (!date.isBefore(windowStart)) count++
            date = frequency.advance(date)
            guard++
        }
        return count
    }

    private companion object {
        const val MAX_OCCURRENCES = 400
    }
}
