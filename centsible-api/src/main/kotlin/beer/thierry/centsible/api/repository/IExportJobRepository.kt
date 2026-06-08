package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ClaimedExportJob
import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.ExportPdf
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import java.util.UUID

interface IExportJobRepository {

    fun enqueue(
        userId: UUID,
        type: ExportType,
        title: String,
        payload: ByteArray,
        postProcessing: List<Pair<PostProcessingType, Map<String, Any?>>>,
    ): ExportJobDTO

    fun fetchByUser(userId: UUID, page: Int, pageSize: Int): List<ExportJobDTO>

    fun fetchById(jobId: UUID, userId: UUID): ExportJobDTO?

    fun fetchPdf(jobId: UUID, userId: UUID): ExportPdf?

    /** Resets the owned job (and its post-processing rows) back to pending, clearing prior PDF, error and lease. */
    fun retrigger(jobId: UUID, userId: UUID): ExportJobDTO?

    fun delete(jobId: UUID, userId: UUID): Boolean

    /**
     * Atomically leases the oldest pending job (or one whose lease lapsed past [leaseTimeoutSeconds]) to
     * [workerId], marking it in-progress and bumping its attempt count; null if none is claimable.
     */
    fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ClaimedExportJob?

    fun fetchByIdForWorker(jobId: UUID): ExportJobDTO?

    fun fetchPdfForWorker(jobId: UUID): ExportPdf?

    fun markCompleted(jobId: UUID, pdf: ByteArray, pdfFilename: String)

    fun markFailed(jobId: UUID, errorMessage: String)
}
