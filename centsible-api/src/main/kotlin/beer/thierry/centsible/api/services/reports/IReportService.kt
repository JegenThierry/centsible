package beer.thierry.centsible.api.services.reports

import beer.thierry.centsible.api.model.reports.AccountBalanceAtDateDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualPeriodDTO
import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.reports.YearOverYearDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate

interface IReportService {
    fun fetchNetWorthOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<NetWorthPointDTO>

    fun fetchAccountBalancesOnDate(
        date: LocalDate,
        authenticatedUser: UserDTO
    ): List<AccountBalanceAtDateDTO>

    fun fetchCategorySpendingOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<CategorySpendingSeriesDTO>

    fun fetchCashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<CashFlowPointDTO>

    fun fetchYearOverYear(authenticatedUser: UserDTO): YearOverYearDTO

    fun fetchBudgetVsActualHistory(
        periods: Int,
        authenticatedUser: UserDTO
    ): List<BudgetVsActualPeriodDTO>
}
