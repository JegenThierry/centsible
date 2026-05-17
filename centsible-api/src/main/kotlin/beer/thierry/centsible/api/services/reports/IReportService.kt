package beer.thierry.centsible.api.services.reports

import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate

interface IReportService {
    fun fetchNetWorthOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<NetWorthPointDTO>
}
