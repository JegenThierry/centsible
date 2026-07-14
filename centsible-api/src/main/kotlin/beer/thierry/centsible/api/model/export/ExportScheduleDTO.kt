package beer.thierry.centsible.api.model.export

import beer.thierry.centsible.api.model.recurring.Frequency
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A recurring export. A materializer enqueues an ordinary export job each period for the just-ended
 * window (a MONTHLY schedule exports last month, WEEKLY exports last week) and advances [nextRunAt].
 */
data class ExportScheduleDTO(
    var id: UUID? = null,
    var userId: UUID? = null,
    var type: ExportType = ExportType.TRANSACTIONS,
    var format: ExportFormat = ExportFormat.PDF,
    var title: String = "",
    var frequency: Frequency? = null,
    var nextRunAt: LocalDate? = null,
    var active: Boolean = true,
    var lastRunAt: OffsetDateTime? = null,
    var createdAt: OffsetDateTime? = null,
    var modifiedAt: OffsetDateTime? = null,
)
