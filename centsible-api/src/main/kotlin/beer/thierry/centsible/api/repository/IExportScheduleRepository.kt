package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate
import java.util.UUID

interface IExportScheduleRepository {
    fun fetchAll(authenticatedUser: UserDTO): List<ExportScheduleDTO>
    fun fetchById(authenticatedUser: UserDTO, id: UUID): ExportScheduleDTO?

    fun create(
        authenticatedUser: UserDTO,
        title: String,
        type: ExportType,
        format: ExportFormat,
        frequency: Frequency,
        nextRunAt: LocalDate,
    ): ExportScheduleDTO

    fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        title: String,
        type: ExportType,
        format: ExportFormat,
        frequency: Frequency,
        active: Boolean,
    ): ExportScheduleDTO?

    fun delete(authenticatedUser: UserDTO, id: UUID): Boolean

    /** Cross-user: active schedules whose next run is on or before [today]. Used by the materializer. */
    fun fetchDue(today: LocalDate): List<ExportScheduleDTO>

    /**
     * Compare-and-swaps an active schedule from [expectedNextRunAt] to [nextRunAt], stamping last_run_at.
     * True only for the caller that won the swap, so concurrent rest instances materialize a due
     * schedule exactly once; false if another instance already advanced it (or it was deactivated).
     */
    fun markRun(id: UUID, expectedNextRunAt: LocalDate, nextRunAt: LocalDate): Boolean
}
