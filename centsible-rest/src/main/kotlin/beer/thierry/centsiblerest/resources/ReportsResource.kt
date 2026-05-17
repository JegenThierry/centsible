package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
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
}
