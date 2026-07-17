package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IExportScheduleRepository
import beer.thierry.jooq.generated.tables.references.EXPORT_SCHEDULES
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ExportScheduleRepository(private val dsl: DSLContext) : IExportScheduleRepository {

    override fun fetchAll(authenticatedUser: UserDTO): List<ExportScheduleDTO> =
        fetchWhere(EXPORT_SCHEDULES.USER_ID.eq(authenticatedUser.id))

    override fun fetchById(authenticatedUser: UserDTO, id: UUID): ExportScheduleDTO? =
        fetchWhere(EXPORT_SCHEDULES.USER_ID.eq(authenticatedUser.id).and(EXPORT_SCHEDULES.ID.eq(id))).firstOrNull()

    override fun create(
        authenticatedUser: UserDTO,
        title: String,
        format: ExportFormat,
        frequency: Frequency,
        nextRunAt: LocalDate,
    ): ExportScheduleDTO {
        val now = OffsetDateTime.now()
        val id = dsl.insertInto(EXPORT_SCHEDULES)
            .set(EXPORT_SCHEDULES.USER_ID, authenticatedUser.id)
            .set(EXPORT_SCHEDULES.TYPE, ExportType.TRANSACTIONS.toJooq())
            .set(EXPORT_SCHEDULES.FORMAT, format.name)
            .set(EXPORT_SCHEDULES.TITLE, title)
            .set(EXPORT_SCHEDULES.FREQUENCY, frequency.name)
            .set(EXPORT_SCHEDULES.NEXT_RUN_AT, nextRunAt)
            .set(EXPORT_SCHEDULES.CREATED_AT, now)
            .set(EXPORT_SCHEDULES.MODIFIED_AT, now)
            .returning(EXPORT_SCHEDULES.ID)
            .fetchOne()
            ?.get(EXPORT_SCHEDULES.ID)
            ?: throw IllegalStateException("Failed to create export schedule")

        return fetchById(authenticatedUser, id)
            ?: throw IllegalStateException("Created export schedule could not be retrieved")
    }

    override fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        title: String,
        format: ExportFormat,
        frequency: Frequency,
        active: Boolean,
    ): ExportScheduleDTO? {
        val updated = dsl.update(EXPORT_SCHEDULES)
            .set(EXPORT_SCHEDULES.TITLE, title)
            .set(EXPORT_SCHEDULES.FORMAT, format.name)
            .set(EXPORT_SCHEDULES.FREQUENCY, frequency.name)
            .set(EXPORT_SCHEDULES.ACTIVE, active)
            .set(EXPORT_SCHEDULES.MODIFIED_AT, OffsetDateTime.now())
            .where(EXPORT_SCHEDULES.USER_ID.eq(authenticatedUser.id).and(EXPORT_SCHEDULES.ID.eq(id)))
            .execute()
        return if (updated == 0) null else fetchById(authenticatedUser, id)
    }

    override fun delete(authenticatedUser: UserDTO, id: UUID): Boolean =
        dsl.deleteFrom(EXPORT_SCHEDULES)
            .where(EXPORT_SCHEDULES.USER_ID.eq(authenticatedUser.id).and(EXPORT_SCHEDULES.ID.eq(id)))
            .execute() > 0

    override fun fetchDue(today: LocalDate): List<ExportScheduleDTO> =
        fetchWhere(EXPORT_SCHEDULES.ACTIVE.isTrue.and(EXPORT_SCHEDULES.NEXT_RUN_AT.le(today)))

    override fun markRun(id: UUID, expectedNextRunAt: LocalDate, nextRunAt: LocalDate): Boolean {
        val now = OffsetDateTime.now()
        // Compare-and-swap on next_run_at: fetchDue takes no lock, so two rest instances firing the same
        // cron both see the schedule as due. Only the instance whose UPDATE still matches the due date
        // advances it — the other gets 0 rows and skips, instead of enqueueing a second export + email.
        return dsl.update(EXPORT_SCHEDULES)
            .set(EXPORT_SCHEDULES.NEXT_RUN_AT, nextRunAt)
            .set(EXPORT_SCHEDULES.LAST_RUN_AT, now)
            .set(EXPORT_SCHEDULES.MODIFIED_AT, now)
            .where(
                EXPORT_SCHEDULES.ID.eq(id)
                    .and(EXPORT_SCHEDULES.NEXT_RUN_AT.eq(expectedNextRunAt))
                    .and(EXPORT_SCHEDULES.ACTIVE.isTrue)
            )
            .execute() == 1
    }

    private fun fetchWhere(condition: Condition): List<ExportScheduleDTO> =
        dsl.selectFrom(EXPORT_SCHEDULES)
            .where(condition)
            .orderBy(EXPORT_SCHEDULES.CREATED_AT.desc())
            .fetch { it.toDTO() }

    private fun Record.toDTO(): ExportScheduleDTO = ExportScheduleDTO(
        id = this[EXPORT_SCHEDULES.ID],
        userId = this[EXPORT_SCHEDULES.USER_ID],
        type = this[EXPORT_SCHEDULES.TYPE]?.toApi() ?: ExportType.TRANSACTIONS,
        format = this[EXPORT_SCHEDULES.FORMAT]?.let { ExportFormat.valueOf(it) } ?: ExportFormat.PDF,
        title = this[EXPORT_SCHEDULES.TITLE] ?: "",
        frequency = this[EXPORT_SCHEDULES.FREQUENCY]?.let { Frequency.valueOf(it) },
        nextRunAt = this[EXPORT_SCHEDULES.NEXT_RUN_AT],
        active = this[EXPORT_SCHEDULES.ACTIVE] ?: true,
        lastRunAt = this[EXPORT_SCHEDULES.LAST_RUN_AT],
        createdAt = this[EXPORT_SCHEDULES.CREATED_AT],
        modifiedAt = this[EXPORT_SCHEDULES.MODIFIED_AT],
    )
}
