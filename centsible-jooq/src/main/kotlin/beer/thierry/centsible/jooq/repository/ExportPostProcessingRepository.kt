package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.export.ExportPostProcessingDTO
import beer.thierry.centsible.api.repository.IExportPostProcessingRepository
import beer.thierry.jooq.generated.enums.ExportStatus as JooqExportStatus
import beer.thierry.jooq.generated.enums.PostProcessingStatus as JooqPostProcessingStatus
import beer.thierry.jooq.generated.tables.references.EXPORT_JOBS
import beer.thierry.jooq.generated.tables.references.EXPORT_POST_PROCESSING
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ExportPostProcessingRepository(
    private val dsl: DSLContext,
    private val ppMapper: ExportPostProcessingMapper,
) : IExportPostProcessingRepository {

    override fun fetchByJob(jobId: UUID): List<ExportPostProcessingDTO> =
        dsl.select(*POST_PROCESSING_COLUMNS)
            .from(EXPORT_POST_PROCESSING)
            .where(EXPORT_POST_PROCESSING.EXPORT_JOB_ID.eq(jobId))
            .orderBy(EXPORT_POST_PROCESSING.CREATED_AT.asc())
            .fetch { ppMapper.toDto(it) }

    @Transactional
    override fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ExportPostProcessingDTO? {
        val now = OffsetDateTime.now()
        val claimable = dsl.select(EXPORT_POST_PROCESSING.ID)
            .from(EXPORT_POST_PROCESSING)
            .join(EXPORT_JOBS).on(EXPORT_JOBS.ID.eq(EXPORT_POST_PROCESSING.EXPORT_JOB_ID))
            .where(
                EXPORT_JOBS.STATUS.eq(JooqExportStatus.COMPLETED).and(
                    EXPORT_POST_PROCESSING.STATUS.eq(JooqPostProcessingStatus.PENDING)
                        .or(
                            EXPORT_POST_PROCESSING.STATUS.eq(JooqPostProcessingStatus.IN_PROGRESS)
                                .and(EXPORT_POST_PROCESSING.LOCKED_AT.lt(now.minusSeconds(leaseTimeoutSeconds)))
                        )
                )
            )
            .orderBy(EXPORT_POST_PROCESSING.CREATED_AT.asc())
            .limit(1)
            .forUpdate().skipLocked()

        val updated = dsl.update(EXPORT_POST_PROCESSING)
            .set(EXPORT_POST_PROCESSING.STATUS, JooqPostProcessingStatus.IN_PROGRESS)
            .set(EXPORT_POST_PROCESSING.LOCKED_AT, now)
            .set(EXPORT_POST_PROCESSING.LOCKED_BY, workerId)
            .set(EXPORT_POST_PROCESSING.ATTEMPT_COUNT, EXPORT_POST_PROCESSING.ATTEMPT_COUNT.plus(1))
            .set(EXPORT_POST_PROCESSING.MODIFIED_AT, now)
            .where(EXPORT_POST_PROCESSING.ID.`in`(claimable))
            .returning()
            .fetchOne() ?: return null

        return ppMapper.toDto(updated)
    }

    override fun markCompleted(id: UUID) {
        val now = OffsetDateTime.now()
        dsl.update(EXPORT_POST_PROCESSING)
            .set(EXPORT_POST_PROCESSING.STATUS, JooqPostProcessingStatus.COMPLETED)
            .setNull(EXPORT_POST_PROCESSING.ERROR_MESSAGE)
            .setNull(EXPORT_POST_PROCESSING.LOCKED_AT)
            .setNull(EXPORT_POST_PROCESSING.LOCKED_BY)
            .set(EXPORT_POST_PROCESSING.COMPLETED_AT, now)
            .set(EXPORT_POST_PROCESSING.MODIFIED_AT, now)
            .where(EXPORT_POST_PROCESSING.ID.eq(id))
            .execute()
    }

    override fun markFailed(id: UUID, errorMessage: String) {
        val now = OffsetDateTime.now()
        dsl.update(EXPORT_POST_PROCESSING)
            .set(EXPORT_POST_PROCESSING.STATUS, JooqPostProcessingStatus.FAILED)
            .set(EXPORT_POST_PROCESSING.ERROR_MESSAGE, errorMessage.take(2000))
            .setNull(EXPORT_POST_PROCESSING.LOCKED_AT)
            .setNull(EXPORT_POST_PROCESSING.LOCKED_BY)
            .set(EXPORT_POST_PROCESSING.MODIFIED_AT, now)
            .where(EXPORT_POST_PROCESSING.ID.eq(id))
            .execute()
    }
}
