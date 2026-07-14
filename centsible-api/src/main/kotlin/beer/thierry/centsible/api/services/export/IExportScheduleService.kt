package beer.thierry.centsible.api.services.export

import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportScheduleForm
import beer.thierry.centsible.api.model.export.ExportScheduleUpdateForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate
import java.util.UUID

interface IExportScheduleService {
    fun list(authenticatedUser: UserDTO): List<ExportScheduleDTO>
    fun create(authenticatedUser: UserDTO, form: ExportScheduleForm): ExportScheduleDTO
    fun update(authenticatedUser: UserDTO, id: UUID, form: ExportScheduleUpdateForm): ExportScheduleDTO?
    fun delete(authenticatedUser: UserDTO, id: UUID): Boolean

    /** Active schedules due on or before [today] (all users), for the materializer to enqueue. */
    fun fetchDue(today: LocalDate): List<ExportScheduleDTO>

    /** Records that a schedule ran and moves it to [nextRunAt]. */
    fun markRun(id: UUID, nextRunAt: LocalDate)
}
