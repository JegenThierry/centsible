package beer.thierry.centsible.core.services.export

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportScheduleForm
import beer.thierry.centsible.api.model.export.ExportScheduleUpdateForm
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IExportScheduleRepository
import beer.thierry.centsible.api.services.export.IExportScheduleService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class ExportScheduleService(
    private val repository: IExportScheduleRepository,
) : IExportScheduleService {

    private val log = LoggerFactory.getLogger(ExportScheduleService::class.java)

    override fun list(authenticatedUser: UserDTO): List<ExportScheduleDTO> =
        repository.fetchAll(authenticatedUser)

    override fun create(authenticatedUser: UserDTO, form: ExportScheduleForm): ExportScheduleDTO {
        assertSchedulable(form.type)
        val frequency = form.frequency
            ?: throw LocalizedException.BadRequest("error.export.schedule.frequencyRequired")
        val nextRunAt = initialNextRun(frequency, LocalDate.now())
        val created = repository.create(authenticatedUser, form.title, form.type, form.format, frequency, nextRunAt)
        log.info(
            "Created export schedule id={} userId={} frequency={} format={} nextRunAt={}",
            created.id, authenticatedUser.id, frequency, form.format, nextRunAt,
        )
        return created
    }

    override fun update(authenticatedUser: UserDTO, id: UUID, form: ExportScheduleUpdateForm): ExportScheduleDTO? {
        assertSchedulable(form.type)
        val frequency = form.frequency
            ?: throw LocalizedException.BadRequest("error.export.schedule.frequencyRequired")
        return repository.update(authenticatedUser, id, form.title, form.type, form.format, frequency, form.active)
    }

    override fun delete(authenticatedUser: UserDTO, id: UUID): Boolean =
        repository.delete(authenticatedUser, id)

    override fun fetchDue(today: LocalDate): List<ExportScheduleDTO> = repository.fetchDue(today)

    override fun markRun(id: UUID, expectedNextRunAt: LocalDate, nextRunAt: LocalDate): Boolean =
        repository.markRun(id, expectedNextRunAt, nextRunAt)

    /**
     * First run aligns to the next calendar boundary so each run's trailing window is a clean period:
     * WEEKLY → next Monday (window = previous Mon–Sun), MONTHLY → 1st of next month (previous calendar
     * month), YEARLY → next Jan 1 (previous year), DAILY → tomorrow (yesterday once it fires).
     */
    private fun initialNextRun(frequency: Frequency, today: LocalDate): LocalDate = when (frequency) {
        Frequency.DAILY -> today.plusDays(1)
        Frequency.WEEKLY -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        Frequency.BIWEEKLY -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1)
        Frequency.MONTHLY -> today.withDayOfMonth(1).plusMonths(1)
        Frequency.YEARLY -> LocalDate.of(today.year + 1, 1, 1)
    }

    private fun assertSchedulable(type: ExportType) {
        if (type == ExportType.LENDINGS_PER_CONTACT) {
            throw LocalizedException.BadRequest("error.export.schedule.typeNotSchedulable")
        }
    }
}
