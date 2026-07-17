package beer.thierry.centsible.core.services.reports

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetPeriodType
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.reports.AccountBalanceAtDateDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualEntryDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualPeriodDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.ForecastOccurrenceDTO
import beer.thierry.centsible.api.model.reports.NetWorthForecastDTO
import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.reports.YearOverYearCategoryDTO
import beer.thierry.centsible.api.model.reports.YearOverYearDTO
import beer.thierry.centsible.api.model.reports.YearOverYearTotalsDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.reports.IReportService
import beer.thierry.centsible.core.services.recurring.occurrenceDatesInWindow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.*

@Service
class ReportService(
    private val reportsRepository: IReportsRepository,
    private val accountsRepository: IBudgetAccountsRepository,
    private val budgetRepository: IBudgetRepository,
    private val currencyConversionService: ICurrencyConversionService,
    private val userRepository: IUserRepository,
    private val recurringRepository: IRecurringTransactionRepository,
) : IReportService {

    private fun ratesFor(authenticatedUser: UserDTO): ConversionRates =
        accountRatesFor(authenticatedUser, accountsRepository, userRepository, currencyConversionService)

    override fun fetchNetWorthOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<NetWorthPointDTO> {
        require(!startDate.isAfter(endDate)) { "Start date must not be after end date" }

        val startOffset = OffsetDateTime.of(startDate, LocalTime.MIN, ZoneOffset.UTC)
        val endOffset = OffsetDateTime.of(endDate, LocalTime.MAX, ZoneOffset.UTC)
        val openingOffset = OffsetDateTime.of(startDate.minusDays(1), LocalTime.MAX, ZoneOffset.UTC)

        val opening = reportsRepository.fetchLatestSnapshotPerAccountAsOf(openingOffset, authenticatedUser)
        val snapshots = reportsRepository.fetchUserSnapshotsBetween(startOffset, endOffset, authenticatedUser)
        val rates = ratesFor(authenticatedUser)

        val accountBalances = mutableMapOf<UUID, BigDecimal>()
        for (snapshot in opening) accountBalances[snapshot.accountId] = snapshot.balance

        val pointByDate = sortedMapOf<LocalDate, BigDecimal>()
        pointByDate[startDate] = totalOf(accountBalances, rates)

        for (snapshot in snapshots) {
            accountBalances[snapshot.accountId] = snapshot.balance
            pointByDate[snapshot.createdAt.toLocalDate()] = totalOf(accountBalances, rates)
        }

        if (pointByDate.lastKey().isBefore(endDate)) {
            pointByDate[endDate] = totalOf(accountBalances, rates)
        }

        return pointByDate.map { (date, balance) -> NetWorthPointDTO(date = date, balance = balance) }
    }

    override fun fetchNetWorthForecast(
        months: Int,
        authenticatedUser: UserDTO,
    ): NetWorthForecastDTO {
        require(months in 1..24) { "months must be 1..24" }

        val today = LocalDate.now()
        val horizonEnd = today.plusMonths(months.toLong())
        val rates = ratesFor(authenticatedUser)
        val accounts = accountsRepository.fetchAllAccounts(authenticatedUser)
        // Forecasting starts from the live net worth (current per-account balances), not the history.
        val startingTotal = totalOf(accounts.associate { it.id to it.balance }, rates)

        // Every upcoming occurrence in [today, horizonEnd], converted to the default currency. Transfers
        // are skipped (they net to zero across accounts); rules whose FX can't be resolved are excluded,
        // mirroring how those accounts drop out of net worth.
        val occurrences = recurringRepository.fetchAll(authenticatedUser)
            .asSequence()
            .filter { it.active && !it.isTransfer }
            .flatMap { rule ->
                val amount = rule.amount
                val type = rule.type
                val rate = rule.accountId?.let { rates.byAccount[it] }
                if (amount == null || type == null || rate == null) {
                    emptySequence()
                } else {
                    val converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                    occurrenceDatesInWindow(rule, today, horizonEnd, MAX_FORECAST_OCCURRENCES).asSequence().map { date ->
                        ForecastOccurrenceDTO(
                            date = date,
                            description = rule.description ?: "",
                            amount = converted,
                            type = type,
                            categoryName = rule.category.name,
                            categoryColor = rule.category.color,
                        )
                    }
                }
            }
            .sortedBy { it.date }
            .toList()

        // Step the running balance forward. Today stays anchored to the actual net worth; occurrences
        // dated today fold into the running total but don't move the anchor point.
        val pointByDate = sortedMapOf(today to startingTotal)
        var running = startingTotal
        for (occ in occurrences) {
            running = when (occ.type) {
                CategoryType.INCOME -> running + occ.amount
                CategoryType.EXPENSE -> running - occ.amount
            }
            if (occ.date.isAfter(today)) {
                pointByDate[occ.date] = running.setScale(2, RoundingMode.HALF_UP)
            }
        }
        // Always extend the line to the horizon so the axis is stable even with no occurrences.
        pointByDate.putIfAbsent(horizonEnd, running.setScale(2, RoundingMode.HALF_UP))

        val points = pointByDate.map { (date, balance) -> NetWorthPointDTO(date = date, balance = balance) }
        return NetWorthForecastDTO(currency = rates.target, points = points, occurrences = occurrences)
    }

    override fun fetchAccountBalancesOnDate(
        date: LocalDate,
        authenticatedUser: UserDTO,
    ): List<AccountBalanceAtDateDTO> {
        val asOf = OffsetDateTime.of(date, LocalTime.MAX, ZoneOffset.UTC)
        val snapshots = reportsRepository.fetchLatestSnapshotPerAccountAsOf(asOf, authenticatedUser)
        val accounts = accountsRepository.fetchAllAccounts(authenticatedUser)
        val rates = ratesFor(authenticatedUser)
        val balances = mutableMapOf<UUID, BigDecimal>()
        for (s in snapshots) balances[s.accountId] = s.balance

        return accounts.map { acc ->
            val raw = balances[acc.id] ?: acc.initialBalance
            AccountBalanceAtDateDTO(
                accountId = acc.id,
                accountName = acc.name,
                currency = acc.currency,
                balance = raw,
                convertedBalance = rates.byAccount[acc.id]?.let { raw.multiply(it).setScale(2, RoundingMode.HALF_UP) },
                targetCurrency = rates.target,
                date = date,
            )
        }
    }

    override fun fetchCategorySpendingOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CategorySpendingSeriesDTO> {
        require(!startDate.isAfter(endDate)) { "Start date must not be after end date" }
        return reportsRepository.fetchCategorySpendingOverTime(startDate, endDate, authenticatedUser)
            .convertedInto(ratesFor(authenticatedUser))
    }

    override fun fetchCashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CashFlowPointDTO> {
        require(!startDate.isAfter(endDate)) { "Start date must not be after end date" }
        return reportsRepository.fetchCashFlow(startDate, endDate, authenticatedUser)
            .convertedInto(ratesFor(authenticatedUser))
    }

    override fun fetchYearOverYear(authenticatedUser: UserDTO): YearOverYearDTO {
        val thisYear = LocalDate.now().year
        val lastYear = thisYear - 1
        val (thisStart, thisEnd) = yearRange(thisYear)
        val (lastStart, lastEnd) = yearRange(lastYear)

        val rates = ratesFor(authenticatedUser)
        val thisYearFlow = reportsRepository.fetchCashFlow(thisStart, thisEnd, authenticatedUser).convertedInto(rates)
        val lastYearFlow = reportsRepository.fetchCashFlow(lastStart, lastEnd, authenticatedUser).convertedInto(rates)
        val thisSeries = reportsRepository.fetchCategorySpendingOverTime(thisStart, thisEnd, authenticatedUser)
            .convertedInto(rates)
        val lastSeries = reportsRepository.fetchCategorySpendingOverTime(lastStart, lastEnd, authenticatedUser)
            .convertedInto(rates)

        val totals = YearOverYearTotalsDTO(
            thisYearIncome = thisYearFlow.sumOf { it.income },
            thisYearExpense = thisYearFlow.sumOf { it.expense },
            lastYearIncome = lastYearFlow.sumOf { it.income },
            lastYearExpense = lastYearFlow.sumOf { it.expense },
        )

        val byCat = mutableMapOf<Long, YoYAccum>()
        for (s in thisSeries) {
            byCat[s.categoryId] = YoYAccum(s.categoryName, s.categoryColor, s.totals.sumOf { it.amount }, BigDecimal.ZERO)
        }
        for (s in lastSeries) {
            val sum = s.totals.sumOf { it.amount }
            byCat[s.categoryId] = byCat[s.categoryId]
                ?.copy(lastYear = sum)
                ?: YoYAccum(s.categoryName, s.categoryColor, BigDecimal.ZERO, sum)
        }

        val perCategory = byCat.map { (id, v) ->
            YearOverYearCategoryDTO(
                categoryId = id,
                categoryName = v.name,
                categoryColor = v.color,
                thisYearAmount = v.thisYear,
                lastYearAmount = v.lastYear,
            )
        }.sortedByDescending { it.thisYearAmount + it.lastYearAmount }

        return YearOverYearDTO(thisYear, lastYear, totals, perCategory)
    }

    private fun yearRange(year: Int): Pair<LocalDate, LocalDate> =
        LocalDate.of(year, 1, 1) to LocalDate.of(year, 12, 31)

    private data class YoYAccum(
        val name: String,
        val color: String?,
        val thisYear: BigDecimal,
        val lastYear: BigDecimal,
    )

    override fun fetchBudgetVsActualHistory(
        periods: Int,
        authenticatedUser: UserDTO,
    ): List<BudgetVsActualPeriodDTO> {
        require(periods in 1..24) { "periods must be 1..24" }
        val current = YearMonth.now()
        val months = (0 until periods).map { current.minusMonths(it.toLong()) }

        return months.map { ym ->
            val budgets = budgetRepository.fetchAllWithSpentForMonth(authenticatedUser, ym)
            val periodType = budgets.firstOrNull()?.periodType ?: BudgetPeriodType.MONTHLY
            val periodKey = budgets.firstOrNull()?.period ?: ym.toString()
            BudgetVsActualPeriodDTO(
                periodKey = periodKey,
                periodType = periodType,
                entries = budgets.map { toEntry(it) },
            )
        }.reversed()
    }

    private fun toEntry(b: BudgetDTO) = BudgetVsActualEntryDTO(
        categoryId = b.category.id ?: 0L,
        categoryName = b.category.name ?: "",
        categoryColor = b.category.color,
        limit = b.amountLimit + b.rolloverAmount,
        spent = b.amountSpent,
    )

    private fun totalOf(balances: Map<UUID, BigDecimal>, rates: ConversionRates): BigDecimal =
        balances.entries
            .sumOf { (accountId, balance) ->
                // Unknown account (e.g. since deleted) keeps face value; unresolvable FX is excluded.
                when (val rate = rates.byAccount.getOrDefault(accountId, BigDecimal.ONE)) {
                    null -> BigDecimal.ZERO
                    else -> balance.multiply(rate)
                }
            }
            .setScale(2, RoundingMode.HALF_UP)

    private companion object {
        // Loop backstop for cadence stepping; comfortably covers daily rules over the 24-month cap.
        const val MAX_FORECAST_OCCURRENCES = 800
    }
}
