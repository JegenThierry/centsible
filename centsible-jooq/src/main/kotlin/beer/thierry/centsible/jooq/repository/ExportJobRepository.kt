package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.export.ClaimedExportJob
import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.ExportPdf
import beer.thierry.centsible.api.model.export.ExportPostProcessingDTO
import beer.thierry.centsible.api.model.export.ExportStatus
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingStatus
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.repository.IExportJobRepository
import beer.thierry.jooq.generated.enums.ExportStatus as JooqExportStatus
import beer.thierry.jooq.generated.enums.ExportType as JooqExportType
import beer.thierry.jooq.generated.enums.PostProcessingStatus as JooqPostProcessingStatus
import beer.thierry.jooq.generated.enums.PostProcessingType as JooqPostProcessingType
import beer.thierry.jooq.generated.tables.references.EXPORT_JOBS
import beer.thierry.jooq.generated.tables.references.EXPORT_POST_PROCESSING
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ExportJobRepository(
    private val dsl: DSLContext,
    private val ppMapper: ExportPostProcessingMapper,
) : IExportJobRepository {

    @Transactional
    override fun enqueue(
        userId: UUID,
        type: ExportType,
        title: String,
        payload: ByteArray,
        postProcessing: List<Pair<PostProcessingType, Map<String, Any?>>>,
    ): ExportJobDTO {
        val record = dsl.insertInto(EXPORT_JOBS)
            .set(EXPORT_JOBS.USER_ID, userId)
            .set(EXPORT_JOBS.TYPE, type.toJooq())
            .set(EXPORT_JOBS.STATUS, JooqExportStatus.PENDING)
            .set(EXPORT_JOBS.TITLE, title)
            .set(EXPORT_JOBS.PAYLOAD, payload)
            .returning()
            .fetchOne() ?: error("Failed to insert export_jobs row")

        val jobId = record[EXPORT_JOBS.ID]!!

        postProcessing.forEach { (ppType, config) ->
            dsl.insertInto(EXPORT_POST_PROCESSING)
                .set(EXPORT_POST_PROCESSING.EXPORT_JOB_ID, jobId)
                .set(EXPORT_POST_PROCESSING.TYPE, ppType.toJooq())
                .set(EXPORT_POST_PROCESSING.STATUS, JooqPostProcessingStatus.PENDING)
                .set(EXPORT_POST_PROCESSING.CONFIG, JSONB.valueOf(ppMapper.writeConfig(config)))
                .execute()
        }

        return fetchById(jobId, userId) ?: error("Unable to read back enqueued job $jobId")
    }

    override fun fetchByUser(userId: UUID, page: Int, pageSize: Int): List<ExportJobDTO> {
        require(page >= 1) { "page must be >= 1" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
        val offset = (page - 1).toLong() * pageSize

        val jobs = dsl.select(*JOB_COLUMNS)
            .from(EXPORT_JOBS)
            .where(EXPORT_JOBS.USER_ID.eq(userId))
            .orderBy(EXPORT_JOBS.CREATED_AT.desc())
            .limit(pageSize).offset(offset)
            .fetch { it.toJobDto(postProcessing = emptyList()) }

        if (jobs.isEmpty()) return jobs

        val postProcessingByJob = fetchPostProcessingFor(jobs.map { it.id }).groupBy { it.exportJobId }
        return jobs.map { it.copy(postProcessing = postProcessingByJob[it.id] ?: emptyList()) }
    }

    override fun fetchById(jobId: UUID, userId: UUID): ExportJobDTO? {
        val row = dsl.select(*JOB_COLUMNS)
            .from(EXPORT_JOBS)
            .where(EXPORT_JOBS.ID.eq(jobId).and(EXPORT_JOBS.USER_ID.eq(userId)))
            .fetchOne() ?: return null
        val pp = fetchPostProcessingFor(listOf(jobId))
        return row.toJobDto(postProcessing = pp)
    }

    override fun fetchByIdForWorker(jobId: UUID): ExportJobDTO? {
        val row = dsl.select(*JOB_COLUMNS)
            .from(EXPORT_JOBS)
            .where(EXPORT_JOBS.ID.eq(jobId))
            .fetchOne() ?: return null
        val pp = fetchPostProcessingFor(listOf(jobId))
        return row.toJobDto(postProcessing = pp)
    }

    override fun fetchPdfForWorker(jobId: UUID): ExportPdf? =
        dsl.select(EXPORT_JOBS.PDF, EXPORT_JOBS.PDF_FILENAME)
            .from(EXPORT_JOBS)
            .where(EXPORT_JOBS.ID.eq(jobId))
            .fetchOne()
            ?.toExportPdf()

    private fun Record.toExportPdf(): ExportPdf? {
        val pdf = this[EXPORT_JOBS.PDF] ?: return null
        return ExportPdf(pdf, this[EXPORT_JOBS.PDF_FILENAME] ?: "export.pdf")
    }

    override fun fetchPdf(jobId: UUID, userId: UUID): ExportPdf? =
        dsl.select(EXPORT_JOBS.PDF, EXPORT_JOBS.PDF_FILENAME)
            .from(EXPORT_JOBS)
            .where(EXPORT_JOBS.ID.eq(jobId).and(EXPORT_JOBS.USER_ID.eq(userId)))
            .fetchOne()
            ?.toExportPdf()

    @Transactional
    override fun retrigger(jobId: UUID, userId: UUID): ExportJobDTO? {
        val updated = dsl.update(EXPORT_JOBS)
            .set(EXPORT_JOBS.STATUS, JooqExportStatus.PENDING)
            .setNull(EXPORT_JOBS.PDF)
            .setNull(EXPORT_JOBS.PDF_FILENAME)
            .setNull(EXPORT_JOBS.ERROR_MESSAGE)
            .setNull(EXPORT_JOBS.LOCKED_AT)
            .setNull(EXPORT_JOBS.LOCKED_BY)
            .setNull(EXPORT_JOBS.COMPLETED_AT)
            .set(EXPORT_JOBS.MODIFIED_AT, OffsetDateTime.now())
            .where(EXPORT_JOBS.ID.eq(jobId).and(EXPORT_JOBS.USER_ID.eq(userId)))
            .execute()
        if (updated == 0) return null

        dsl.update(EXPORT_POST_PROCESSING)
            .set(EXPORT_POST_PROCESSING.STATUS, JooqPostProcessingStatus.PENDING)
            .setNull(EXPORT_POST_PROCESSING.ERROR_MESSAGE)
            .setNull(EXPORT_POST_PROCESSING.LOCKED_AT)
            .setNull(EXPORT_POST_PROCESSING.LOCKED_BY)
            .setNull(EXPORT_POST_PROCESSING.COMPLETED_AT)
            .set(EXPORT_POST_PROCESSING.MODIFIED_AT, OffsetDateTime.now())
            .where(EXPORT_POST_PROCESSING.EXPORT_JOB_ID.eq(jobId))
            .execute()

        return fetchById(jobId, userId)
    }

    override fun delete(jobId: UUID, userId: UUID): Boolean {
        return dsl.deleteFrom(EXPORT_JOBS)
            .where(EXPORT_JOBS.ID.eq(jobId).and(EXPORT_JOBS.USER_ID.eq(userId)))
            .execute() > 0
    }

    @Transactional
    override fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ClaimedExportJob? {
        val now = OffsetDateTime.now()
        val claimable = dsl.select(EXPORT_JOBS.ID)
            .from(EXPORT_JOBS)
            .where(
                EXPORT_JOBS.STATUS.eq(JooqExportStatus.PENDING)
                    .or(
                        EXPORT_JOBS.STATUS.eq(JooqExportStatus.IN_PROGRESS)
                            .and(EXPORT_JOBS.LOCKED_AT.lt(now.minusSeconds(leaseTimeoutSeconds)))
                    )
            )
            .orderBy(EXPORT_JOBS.CREATED_AT.asc())
            .limit(1)
            .forUpdate().skipLocked()

        val updated = dsl.update(EXPORT_JOBS)
            .set(EXPORT_JOBS.STATUS, JooqExportStatus.IN_PROGRESS)
            .set(EXPORT_JOBS.LOCKED_AT, now)
            .set(EXPORT_JOBS.LOCKED_BY, workerId)
            .set(EXPORT_JOBS.ATTEMPT_COUNT, EXPORT_JOBS.ATTEMPT_COUNT.plus(1))
            .set(EXPORT_JOBS.MODIFIED_AT, now)
            .where(EXPORT_JOBS.ID.`in`(claimable))
            .returning()
            .fetchOne() ?: return null

        val userId = updated[EXPORT_JOBS.USER_ID]!!
        val payload = updated[EXPORT_JOBS.PAYLOAD]!!
        val job = fetchById(updated[EXPORT_JOBS.ID]!!, userId) ?: return null
        return ClaimedExportJob(job, payload)
    }

    override fun markCompleted(jobId: UUID, pdf: ByteArray, pdfFilename: String) {
        val now = OffsetDateTime.now()
        dsl.update(EXPORT_JOBS)
            .set(EXPORT_JOBS.STATUS, JooqExportStatus.COMPLETED)
            .set(EXPORT_JOBS.PDF, pdf)
            .set(EXPORT_JOBS.PDF_FILENAME, pdfFilename)
            .setNull(EXPORT_JOBS.ERROR_MESSAGE)
            .setNull(EXPORT_JOBS.LOCKED_AT)
            .setNull(EXPORT_JOBS.LOCKED_BY)
            .set(EXPORT_JOBS.COMPLETED_AT, now)
            .set(EXPORT_JOBS.MODIFIED_AT, now)
            .where(EXPORT_JOBS.ID.eq(jobId))
            .execute()
    }

    override fun markFailed(jobId: UUID, errorMessage: String) {
        val now = OffsetDateTime.now()
        dsl.update(EXPORT_JOBS)
            .set(EXPORT_JOBS.STATUS, JooqExportStatus.FAILED)
            .set(EXPORT_JOBS.ERROR_MESSAGE, errorMessage.take(2000))
            .setNull(EXPORT_JOBS.LOCKED_AT)
            .setNull(EXPORT_JOBS.LOCKED_BY)
            .set(EXPORT_JOBS.MODIFIED_AT, now)
            .where(EXPORT_JOBS.ID.eq(jobId))
            .execute()
    }

    private fun fetchPostProcessingFor(jobIds: List<UUID>): List<ExportPostProcessingDTO> {
        if (jobIds.isEmpty()) return emptyList()
        return dsl.select(*POST_PROCESSING_COLUMNS)
            .from(EXPORT_POST_PROCESSING)
            .where(EXPORT_POST_PROCESSING.EXPORT_JOB_ID.`in`(jobIds))
            .orderBy(EXPORT_POST_PROCESSING.CREATED_AT.asc())
            .fetch { ppMapper.toDto(it) }
    }

    private fun Record.toJobDto(postProcessing: List<ExportPostProcessingDTO>): ExportJobDTO =
        ExportJobDTO(
            id = this[EXPORT_JOBS.ID]!!,
            userId = this[EXPORT_JOBS.USER_ID]!!,
            type = this[EXPORT_JOBS.TYPE]!!.toApi(),
            status = this[EXPORT_JOBS.STATUS]!!.toApi(),
            title = this[EXPORT_JOBS.TITLE]!!,
            pdfFilename = this[EXPORT_JOBS.PDF_FILENAME],
            errorMessage = this[EXPORT_JOBS.ERROR_MESSAGE],
            attemptCount = this[EXPORT_JOBS.ATTEMPT_COUNT]!!,
            completedAt = this[EXPORT_JOBS.COMPLETED_AT],
            createdAt = this[EXPORT_JOBS.CREATED_AT]!!,
            modifiedAt = this[EXPORT_JOBS.MODIFIED_AT]!!,
            postProcessing = postProcessing.filter { it.exportJobId == this[EXPORT_JOBS.ID] },
        )

    companion object {
        private val JOB_COLUMNS = arrayOf(
            EXPORT_JOBS.ID,
            EXPORT_JOBS.USER_ID,
            EXPORT_JOBS.TYPE,
            EXPORT_JOBS.STATUS,
            EXPORT_JOBS.TITLE,
            EXPORT_JOBS.PDF_FILENAME,
            EXPORT_JOBS.ERROR_MESSAGE,
            EXPORT_JOBS.ATTEMPT_COUNT,
            EXPORT_JOBS.COMPLETED_AT,
            EXPORT_JOBS.CREATED_AT,
            EXPORT_JOBS.MODIFIED_AT,
        )
    }
}

internal fun ExportType.toJooq(): JooqExportType = when (this) {
    ExportType.TRANSACTIONS -> JooqExportType.TRANSACTIONS
    ExportType.LENDINGS_PER_CONTACT -> JooqExportType.LENDINGS_PER_CONTACT
    ExportType.LENDINGS_ALL -> JooqExportType.LENDINGS_ALL
    ExportType.ACCOUNTS_SUMMARY -> JooqExportType.ACCOUNTS_SUMMARY
}

internal fun JooqExportType.toApi(): ExportType = when (this) {
    JooqExportType.TRANSACTIONS -> ExportType.TRANSACTIONS
    JooqExportType.LENDINGS_PER_CONTACT -> ExportType.LENDINGS_PER_CONTACT
    JooqExportType.LENDINGS_ALL -> ExportType.LENDINGS_ALL
    JooqExportType.ACCOUNTS_SUMMARY -> ExportType.ACCOUNTS_SUMMARY
}

internal fun JooqExportStatus.toApi(): ExportStatus = when (this) {
    JooqExportStatus.PENDING -> ExportStatus.PENDING
    JooqExportStatus.IN_PROGRESS -> ExportStatus.IN_PROGRESS
    JooqExportStatus.COMPLETED -> ExportStatus.COMPLETED
    JooqExportStatus.FAILED -> ExportStatus.FAILED
}

internal fun PostProcessingType.toJooq(): JooqPostProcessingType = when (this) {
    PostProcessingType.SEND_EMAIL -> JooqPostProcessingType.SEND_EMAIL
}

internal fun JooqPostProcessingType.toApi(): PostProcessingType = when (this) {
    JooqPostProcessingType.SEND_EMAIL -> PostProcessingType.SEND_EMAIL
}

internal fun JooqPostProcessingStatus.toApi(): PostProcessingStatus = when (this) {
    JooqPostProcessingStatus.PENDING -> PostProcessingStatus.PENDING
    JooqPostProcessingStatus.IN_PROGRESS -> PostProcessingStatus.IN_PROGRESS
    JooqPostProcessingStatus.COMPLETED -> PostProcessingStatus.COMPLETED
    JooqPostProcessingStatus.FAILED -> PostProcessingStatus.FAILED
}
