package beer.thierry.centsible.core.services.reports

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.reports.SafeToSpendDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.reports.ISafeToSpendService
import beer.thierry.centsible.core.services.recurring.occurrenceDatesInWindow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
class SafeToSpendService(
    private val reportsRepository: IReportsRepository,
    private val recurringRepository: IRecurringTransactionRepository,
    private val accountsRepository: IBudgetAccountsRepository,
    private val userRepository: IUserRepository,
    private val currencyConversionService: ICurrencyConversionService,
) : ISafeToSpendService {

    override fun fetchForCurrentMonth(authenticatedUser: UserDTO): SafeToSpendDTO {
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val rates = accountRatesFor(authenticatedUser, accountsRepository, userRepository, currencyConversionService)
        val cashFlow = reportsRepository.fetchCashFlow(monthStart, monthEnd, authenticatedUser).convertedInto(rates)
        val actualIncome = cashFlow.sumOf { it.income }
        val alreadySpent = cashFlow.sumOf { it.expense }

        var upcomingIncome = BigDecimal.ZERO
        var upcomingExpenses = BigDecimal.ZERO
        for (rule in recurringRepository.fetchAll(authenticatedUser)) {
            if (!rule.active || rule.isTransfer) continue
            val amount = rule.amount ?: continue
            val type = rule.type ?: continue
            val rate = rule.accountId?.let { rates.byAccount[it] } ?: continue
            val occurrences = occurrencesInWindow(rule, today, monthEnd)
            if (occurrences == 0) continue
            val total = amount.multiply(rate).multiply(occurrences.toBigDecimal())
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
            currency = rates.target.name,
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
    ): Int = occurrenceDatesInWindow(rule, windowStart, windowEnd, MAX_OCCURRENCES).size

    private companion object {
        const val MAX_OCCURRENCES = 400
    }
}
