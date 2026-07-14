package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportScheduleDTO
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
        format: ExportFormat,
        frequency: Frequency,
        nextRunAt: LocalDate,
    ): ExportScheduleDTO

    fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        title: String,
        format: ExportFormat,
        frequency: Frequency,
        active: Boolean,
    ): ExportScheduleDTO?

    fun delete(authenticatedUser: UserDTO, id: UUID): Boolean

    /** Cross-user: active schedules whose next run is on or before [today]. Used by the materializer. */
    fun fetchDue(today: LocalDate): List<ExportScheduleDTO>

    /** Advances a schedule after a run: sets next_run_at and stamps last_run_at. */
    fun markRun(id: UUID, nextRunAt: LocalDate)
}
