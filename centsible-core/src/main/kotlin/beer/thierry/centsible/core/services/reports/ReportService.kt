package beer.thierry.centsible.core.services.reports

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetPeriodType
import beer.thierry.centsible.api.model.reports.AccountBalanceAtDateDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualEntryDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualPeriodDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.reports.YearOverYearCategoryDTO
import beer.thierry.centsible.api.model.reports.YearOverYearDTO
import beer.thierry.centsible.api.model.reports.YearOverYearTotalsDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.api.services.reports.IReportService
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
) : IReportService {

    override fun fetchNetWorthOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<NetWorthPointDTO> {
        require(!startDate.isAfter(endDate)) { "Start date must not be after end date" }

        val endOffset = OffsetDateTime.of(endDate, LocalTime.MAX, ZoneOffset.UTC)
        val snapshots = reportsRepository.fetchAllUserSnapshotsUntil(endOffset, authenticatedUser)

        val accountBalances = mutableMapOf<UUID, BigDecimal>()
        val pointByDate = sortedMapOf<LocalDate, BigDecimal>()
        var crossedStart = false

        for (snapshot in snapshots) {
            val date = snapshot.createdAt.toLocalDate()
            if (!crossedStart && !date.isBefore(startDate)) {
                pointByDate[startDate] = totalOf(accountBalances)
                crossedStart = true
            }
            accountBalances[snapshot.accountId] = snapshot.balance
            if (!date.isBefore(startDate)) {
                pointByDate[date] = totalOf(accountBalances)
            }
        }

        if (pointByDate.isEmpty()) {
            val total = totalOf(accountBalances)
            pointByDate[startDate] = total
            pointByDate[endDate] = total
        } else if (pointByDate.lastKey().isBefore(endDate)) {
            pointByDate[endDate] = totalOf(accountBalances)
        }

        return pointByDate.map { (date, balance) -> NetWorthPointDTO(date = date, balance = balance) }
    }

    override fun fetchAccountBalancesOnDate(
        date: LocalDate,
        authenticatedUser: UserDTO,
    ): List<AccountBalanceAtDateDTO> {
        val asOf = OffsetDateTime.of(date, LocalTime.MAX, ZoneOffset.UTC)
        val snapshots = reportsRepository.fetchAllUserSnapshotsUntil(asOf, authenticatedUser)
        val accounts = accountsRepository.fetchAllAccounts(authenticatedUser)
        val balances = mutableMapOf<UUID, BigDecimal>()
        for (s in snapshots) balances[s.accountId] = s.balance

        return accounts.map { acc ->
            val raw = balances[acc.id] ?: acc.initialBalance
            AccountBalanceAtDateDTO(
                accountId = acc.id,
                accountName = acc.name,
                currency = acc.currency,
                balance = raw,
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
    }

    override fun fetchCashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CashFlowPointDTO> {
        require(!startDate.isAfter(endDate)) { "Start date must not be after end date" }
        return reportsRepository.fetchCashFlow(startDate, endDate, authenticatedUser)
    }

    override fun fetchYearOverYear(authenticatedUser: UserDTO): YearOverYearDTO {
        val thisYear = LocalDate.now().year
        val lastYear = thisYear - 1
        val (thisStart, thisEnd) = yearRange(thisYear)
        val (lastStart, lastEnd) = yearRange(lastYear)

        val thisYearFlow = reportsRepository.fetchCashFlow(thisStart, thisEnd, authenticatedUser)
        val lastYearFlow = reportsRepository.fetchCashFlow(lastStart, lastEnd, authenticatedUser)
        val thisSeries = reportsRepository.fetchCategorySpendingOverTime(thisStart, thisEnd, authenticatedUser)
        val lastSeries = reportsRepository.fetchCategorySpendingOverTime(lastStart, lastEnd, authenticatedUser)

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

    private fun totalOf(balances: Map<UUID, BigDecimal>): BigDecimal =
        balances.values.sumOf { it }
}
