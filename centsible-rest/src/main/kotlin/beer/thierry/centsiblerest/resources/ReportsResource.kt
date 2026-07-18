package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.reports.AccountBalanceAtDateDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualPeriodDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.NetWorthForecastDTO
import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.reports.YearOverYearDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.reports.IReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RequestMapping("/api/reports")
@RestController
class ReportsResource(private val reportService: IReportService) {

    @GetMapping("/net-worth")
    fun fetchNetWorth(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<NetWorthPointDTO> =
        reportService.fetchNetWorthOverTime(startDate, endDate, authenticatedUser)

    @GetMapping("/net-worth/breakdown")
    fun fetchNetWorthBreakdown(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<AccountBalanceAtDateDTO> =
        reportService.fetchAccountBalancesOnDate(date, authenticatedUser)

    @GetMapping("/category-spending")
    fun fetchCategorySpending(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<CategorySpendingSeriesDTO> =
        reportService.fetchCategorySpendingOverTime(startDate, endDate, authenticatedUser)

    @GetMapping("/cash-flow")
    fun fetchCashFlow(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<CashFlowPointDTO> =
        reportService.fetchCashFlow(startDate, endDate, authenticatedUser)

    @GetMapping("/forecast")
    fun fetchForecast(
        @RequestParam(defaultValue = "6") months: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): NetWorthForecastDTO {
        if (months !in 1..24) throw LocalizedException.BadRequest("error.report.monthsOutOfRange")
        return reportService.fetchNetWorthForecast(months, authenticatedUser)
    }

    @GetMapping("/year-over-year")
    fun fetchYearOverYear(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): YearOverYearDTO =
        reportService.fetchYearOverYear(authenticatedUser)

    @GetMapping("/budget-vs-actual")
    fun fetchBudgetVsActual(
        @RequestParam(defaultValue = "6") periods: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<BudgetVsActualPeriodDTO> {
        if (periods !in 1..24) throw LocalizedException.BadRequest("error.report.periodsOutOfRange")
        return reportService.fetchBudgetVsActualHistory(periods, authenticatedUser)
    }
}
