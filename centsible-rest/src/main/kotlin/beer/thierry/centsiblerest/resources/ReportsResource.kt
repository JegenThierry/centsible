package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.reports.AccountBalanceAtDateDTO
import beer.thierry.centsible.api.model.reports.BudgetVsActualPeriodDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.reports.YearOverYearDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.reports.IReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<List<NetWorthPointDTO>> =
        ResponseEntity.ok(reportService.fetchNetWorthOverTime(startDate, endDate, authenticatedUser))

    @GetMapping("/net-worth/breakdown")
    fun fetchNetWorthBreakdown(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<AccountBalanceAtDateDTO>> =
        ResponseEntity.ok(reportService.fetchAccountBalancesOnDate(date, authenticatedUser))

    @GetMapping("/category-spending")
    fun fetchCategorySpending(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<CategorySpendingSeriesDTO>> =
        ResponseEntity.ok(reportService.fetchCategorySpendingOverTime(startDate, endDate, authenticatedUser))

    @GetMapping("/cash-flow")
    fun fetchCashFlow(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<CashFlowPointDTO>> =
        ResponseEntity.ok(reportService.fetchCashFlow(startDate, endDate, authenticatedUser))

    @GetMapping("/year-over-year")
    fun fetchYearOverYear(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<YearOverYearDTO> =
        ResponseEntity.ok(reportService.fetchYearOverYear(authenticatedUser))

    @GetMapping("/budget-vs-actual")
    fun fetchBudgetVsActual(
        @RequestParam(defaultValue = "6") periods: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<BudgetVsActualPeriodDTO>> {
        if (periods !in 1..24) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(reportService.fetchBudgetVsActualHistory(periods, authenticatedUser))
    }
}
