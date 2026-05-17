package beer.thierry.centsible.core.services.reports

import beer.thierry.centsible.api.model.reports.NetWorthPointDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.api.services.reports.IReportService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ReportService(
    private val reportsRepository: IReportsRepository,
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
                // Capture the opening total from everything that happened strictly before startDate.
                pointByDate[startDate] = totalOf(accountBalances)
                crossedStart = true
            }
            accountBalances[snapshot.accountId] = snapshot.balance
            if (!date.isBefore(startDate)) {
                // Last write wins for same-day events — keeps the series at most one point per day.
                pointByDate[date] = totalOf(accountBalances)
            }
        }

        if (pointByDate.isEmpty()) {
            // No events in range: anchor a flat line at the current total.
            val total = totalOf(accountBalances)
            pointByDate[startDate] = total
            pointByDate[endDate] = total
        } else if (pointByDate.lastKey().isBefore(endDate)) {
            pointByDate[endDate] = totalOf(accountBalances)
        }

        return pointByDate.map { (date, balance) -> NetWorthPointDTO(date = date, balance = balance) }
    }

    private fun totalOf(balances: Map<UUID, BigDecimal>): BigDecimal =
        balances.values.fold(BigDecimal.ZERO, BigDecimal::add)
}
